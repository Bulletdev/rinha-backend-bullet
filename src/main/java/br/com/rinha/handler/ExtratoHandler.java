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

/**
 * Handler para processar requisições de extrato
 */
public class ExtratoHandler {
    private final ClienteRepository clienteRepository;
    private final TransacaoRepository transacaoRepository;

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
        try {
            // Verificar se o cliente existe
            Cliente cliente = clienteRepository.findById(clientId);

            if (cliente == null) {
                sendResponse(exchange, 404, "Cliente não encontrado");
                return;
            }

            // Obter as últimas transações
            List<Transacao> transacoes = transacaoRepository.getLatestTransactions(clientId);

            // Criar resposta JSON
            ObjectNode response = JsonUtil.createExtractResponse(cliente, transacoes);

            // Adicionar cabeçalho de cache para melhorar desempenho
            exchange.getResponseHeaders().set("Cache-Control", "public, max-age=5");

            // Enviar resposta
            sendJsonResponse(exchange, 200, response);

        } catch (SQLException e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "Erro no banco de dados: " + e.getMessage());
        }
    }

    // Métodos utilitários para envio de respostas
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
}