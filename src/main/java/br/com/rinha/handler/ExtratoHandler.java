package br.com.rinha.handler;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import br.com.rinha.model.Cliente;
import br.com.rinha.model.Transacao;
import br.com.rinha.repository.ClienteRepository;
import br.com.rinha.repository.TransacaoRepository;
import br.com.rinha.util.JsonUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Handler para processar requisições de extrato
 * Otimizado para alta concorrência
 */
public class ExtratoHandler {
    private final ClienteRepository clienteRepository;
    private final TransacaoRepository transacaoRepository;

    // Cache para clientes inexistentes para economizar consultas
    private final ConcurrentHashMap<Integer, Boolean> nonExistentClients = new ConcurrentHashMap<>();

    // Cache para respostas de extrato recentes (cache de 5 segundos)
    private final ConcurrentHashMap<Integer, CachedExtrato> extratoCache = new ConcurrentHashMap<>();

    public ExtratoHandler() {
        this.clienteRepository = new ClienteRepository();
        this.transacaoRepository = new TransacaoRepository();
    }

    /**
     * Processa uma requisição de extrato
     * @param exchange Objeto de troca HTTP
     * @param clientId ID do cliente
     * @throws IOException em caso de erro de I/O
     */
    public void handle(HttpExchange exchange, int clientId) throws IOException {
        long startTime = System.nanoTime();

        // Verifica cache de clientes inexistentes para respostas mais rápidas
        if (nonExistentClients.containsKey(clientId)) {
            sendResponse(exchange, 404, "Cliente não encontrado");
            return;
        }

        // Verifica se há uma resposta em cache válida
        CachedExtrato cachedExtrato = extratoCache.get(clientId);
        if (cachedExtrato != null && !cachedExtrato.isExpired()) {
            // Usa a resposta em cache
            exchange.getResponseHeaders().set("X-Cache", "HIT");
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Cache-Control", "public, max-age=5");

            byte[] response = cachedExtrato.getResponseBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
            return;
        }

        try {
            // Obter o cliente (usando cache na repository)
            Cliente cliente = clienteRepository.findById(clientId);

            if (cliente == null) {
                // Armazena cliente inexistente no cache
                nonExistentClients.put(clientId, Boolean.TRUE);
                sendResponse(exchange, 404, "Cliente não encontrado");
                return;
            }

            // Obter as últimas transações
            List<Transacao> transacoes = transacaoRepository.getLatestTransactions(clientId);

            // Criar resposta JSON
            ObjectNode response = JsonUtil.createExtractResponse(cliente, transacoes);

            // Serializar resposta para bytes
            byte[] responseBytes = JsonUtil.getObjectMapper().writeValueAsBytes(response);

            // Armazenar no cache
            extratoCache.put(clientId, new CachedExtrato(responseBytes));

            // Adicionar cabeçalhos
            exchange.getResponseHeaders().set("X-Cache", "MISS");
            exchange.getResponseHeaders().set("Cache-Control", "public, max-age=5");
            exchange.getResponseHeaders().set("Content-Type", "application/json");

            // Enviar resposta
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }

            // Registrar métrica de tempo
            long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            if (duration > 100) {  // Log apenas consultas lentas
                System.out.println("Extrato lento para cliente " + clientId + ": " + duration + "ms");
            }

        } catch (SQLException e) {
            // Log detalhado do erro
            System.err.println("Erro SQL ao obter extrato: " + e.getMessage());
            e.printStackTrace();

            // Resposta específica para diferentes tipos de erros SQL
            if (e.getMessage().contains("deadlock") || e.getMessage().contains("timeout")) {
                sendResponse(exchange, 503, "Serviço temporariamente indisponível, tente novamente");
            } else {
                sendResponse(exchange, 500, "Erro interno do servidor: " + e.getMessage());
            }
        } catch (Exception e) {
            // Log de erro genérico
            System.err.println("Erro não esperado ao obter extrato: " + e.getMessage());
            e.printStackTrace();
            sendResponse(exchange, 500, "Erro interno do servidor");
        }
    }

    /**
     * Classe interna para armazenar respostas em cache
     */
    private static class CachedExtrato {
        private final byte[] responseBytes;
        private final long timestamp;
        private static final long TTL_MS = 5000; // Cache por 5 segundos

        public CachedExtrato(byte[] responseBytes) {
            this.responseBytes = responseBytes;
            this.timestamp = System.currentTimeMillis();
        }

        public byte[] getResponseBytes() {
            return responseBytes;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > TTL_MS;
        }
    }

    // Métodos utilitários

    private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        byte[] responseBytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}