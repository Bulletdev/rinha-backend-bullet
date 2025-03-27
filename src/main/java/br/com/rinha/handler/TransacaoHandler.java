package br.com.rinha.handler;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import br.com.rinha.model.Transacao;
import br.com.rinha.repository.ClienteRepository;
import br.com.rinha.repository.TransacaoRepository;
import br.com.rinha.util.JsonUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handler para processar requisições de transações
 * Otimizado para alta concorrência
 */
public class TransacaoHandler {
    private final ClienteRepository clienteRepository;
    private final TransacaoRepository transacaoRepository;

    // Métricas para monitoramento de desempenho
    private final ConcurrentHashMap<Integer, AtomicLong> clientRequestCount = new ConcurrentHashMap<>();

    // Cache para clientes inexistentes para economizar consultas
    private final ConcurrentHashMap<Integer, Boolean> nonExistentClients = new ConcurrentHashMap<>();

    public TransacaoHandler() {
        this.clienteRepository = new ClienteRepository();
        this.transacaoRepository = new TransacaoRepository();

        // Pré-carrega os clientes no cache para melhorar desempenho
        try {
            clienteRepository.preloadClientCache();
        } catch (SQLException e) {
            System.err.println("Erro ao pré-carregar cache de clientes: " + e.getMessage());
        }
    }

    /**
     * Processa uma requisição de transação
     * @param exchange Objeto de troca HTTP
     * @param clientId ID do cliente
     * @throws IOException em caso de erro de I/O
     */
    public void handle(HttpExchange exchange, int clientId) throws IOException {
        long startTime = System.nanoTime();

        // Incrementa contador de requisições para o cliente
        clientRequestCount.computeIfAbsent(clientId, k -> new AtomicLong()).incrementAndGet();

        // Verifica cache de clientes inexistentes para respostas mais rápidas
        if (nonExistentClients.containsKey(clientId)) {
            sendResponse(exchange, 404, "Cliente não encontrado");
            return;
        }

        // Ler o corpo da requisição de forma otimizada
        byte[] requestBodyBytes;
        try {
            requestBodyBytes = exchange.getRequestBody().readAllBytes();
        } catch (IOException e) {
            sendResponse(exchange, 400, "Erro ao ler corpo da requisição");
            return;
        }

        // Validar e analisar a requisição
        if (requestBodyBytes.length == 0) {
            sendResponse(exchange, 422, "Corpo da requisição vazio");
            return;
        }

        ObjectNode transactionJson;
        try {
            transactionJson = JsonUtil.getObjectMapper().readValue(requestBodyBytes, ObjectNode.class);
        } catch (Exception e) {
            sendResponse(exchange, 422, "Formato JSON inválido");
            return;
        }

        // Extrair e validar os campos da transação
        if (!isValidTransactionJson(transactionJson)) {
            sendResponse(exchange, 422, "Dados da transação inválidos");
            return;
        }

        int valor = transactionJson.get("valor").asInt();
        String tipo = transactionJson.get("tipo").asText();
        String descricao = transactionJson.get("descricao").asText();

        try {
            // Verificar se o cliente existe (usando cache para performance)
            if (!clienteRepository.clientExists(clientId)) {
                // Armazena cliente inexistente no cache
                nonExistentClients.put(clientId, Boolean.TRUE);
                sendResponse(exchange, 404, "Cliente não encontrado");
                return;
            }

            // Processar a transação usando atualização atômica com retry
            int[] result = clienteRepository.atomicUpdate(clientId, tipo, valor);
            int saldo = result[0];
            int limite = result[1];
            int success = result[2];

            if (success == 0) {
                sendResponse(exchange, 422, "Saldo insuficiente");
                return;
            }

            // Registrar a transação no histórico (assíncrono para não bloquear a resposta)
            try {
                Transacao transacao = new Transacao(clientId, valor, tipo, descricao);
                transacaoRepository.saveAsync(transacao);
            } catch (Exception e) {
                // Log do erro, mas não falha a requisição principal
                System.err.println("Erro ao salvar transação assincronamente: " + e.getMessage());
                e.printStackTrace();
            }

            // Enviar resposta otimizada
            ObjectNode response = JsonUtil.createTransactionResponse(limite, saldo);

            // Adicionar cabeçalho de cache para clientes
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.getResponseHeaders().set("Content-Type", "application/json");

            sendJsonResponse(exchange, 200, response);

            // Registrar métrica de tempo
            long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            if (duration > 200) {  // Log apenas transações lentas
                System.out.println("Transação lenta para cliente " + clientId + ": " + duration + "ms");
            }

        } catch (SQLException e) {
            // Log detalhado do erro
            System.err.println("Erro SQL ao processar transação: " + e.getMessage());
            e.printStackTrace();

            // Resposta específica para diferentes tipos de erros SQL
            if (e.getMessage().contains("deadlock") || e.getMessage().contains("timeout")) {
                sendResponse(exchange, 503, "Serviço temporariamente indisponível, tente novamente");
            } else {
                sendResponse(exchange, 500, "Erro interno do servidor: " + e.getMessage());
            }
        } catch (Exception e) {
            // Log de erro genérico
            System.err.println("Erro não esperado ao processar transação: " + e.getMessage());
            e.printStackTrace();
            sendResponse(exchange, 500, "Erro interno do servidor");
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

    private void sendJsonResponse(HttpExchange exchange, int statusCode, ObjectNode json) throws IOException {
        byte[] response = JsonUtil.getObjectMapper().writeValueAsBytes(json);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private boolean isValidTransactionJson(ObjectNode json) {
        try {
            // Verificar se todos os campos obrigatórios estão presentes
            if (!json.has("valor") || !json.has("tipo") || !json.has("descricao")) {
                return false;
            }

            // Validar valor
            if (!json.get("valor").isInt() || json.get("valor").asInt() <= 0) {
                return false;
            }

            // Validar tipo
            String type = json.get("tipo").asText();
            if (!"c".equals(type) && !"d".equals(type)) {
                return false;
            }

            // Validar descrição
            String description = json.get("descricao").asText();
            return description != null && description.length() >= 1 && description.length() <= 10;
        } catch (Exception e) {
            return false;
        }
    }
}