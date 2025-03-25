package br.com.rinha.handler;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import br.com.rinha.config.DatabaseConfig;
import br.com.rinha.model.Transacao;
import br.com.rinha.repository.ClienteRepository;
import br.com.rinha.repository.TransacaoRepository;
import br.com.rinha.util.JsonUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;

/**
 * Handler para processar requisições de transações
 */
public class TransacaoHandler {
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
    private final ClienteRepository clienteRepository;
    private final TransacaoRepository transacaoRepository;

    public TransacaoHandler() {
        this.clienteRepository = new ClienteRepository();
        this.transacaoRepository = new TransacaoRepository();
    }

    /**
     * Processa uma requisição de transação
     * @param exchange Objeto de troca HTTP
     * @param clientId ID do cliente
     * @throws IOException em caso de erro de I/O
     */
    public void handle(HttpExchange exchange, int clientId) throws IOException {
        // Ler o corpo da requisição
        byte[] requestBodyBytes = exchange.getRequestBody().readAllBytes();

        // Validar e analisar a requisição
        if (requestBodyBytes.length == 0) {
            sendResponse(exchange, 422, "Empty request body");
            return;
        }

        ObjectNode transactionJson;
        try {
            transactionJson = JsonUtil.getObjectMapper().readValue(requestBodyBytes, ObjectNode.class);
        } catch (Exception e) {
            sendResponse(exchange, 422, "Invalid JSON format");
            return;
        }

        // Extrair e validar os campos da transação
        if (!isValidTransactionJson(transactionJson)) {
            sendResponse(exchange, 422, "Invalid transaction data");
            return;
        }

        int valor = transactionJson.get("valor").asInt();
        String tipo = transactionJson.get("tipo").asText();
        String descricao = transactionJson.get("descricao").asText();

        try {
            // Verificar se o cliente existe
            if (!clienteRepository.clientExists(clientId)) {
                sendResponse(exchange, 404, "Cliente não encontrado");
                return;
            }

            // Processar a transação usando atualização atômica
            int[] result = clienteRepository.atomicUpdate(clientId, tipo, valor);
            int saldo = result[0];
            int limite = result[1];
            int success = result[2];

            if (success == 0) {
                sendResponse(exchange, 422, "Saldo insuficiente");
                return;
            }

            // Registrar a transação no histórico
            Transacao transacao = new Transacao(clientId, valor, tipo, descricao);
            transacaoRepository.save(transacao);

            // Enviar resposta
            ObjectNode response = JsonUtil.createTransactionResponse(limite, saldo);
            sendJsonResponse(exchange, 200, response);

        } catch (SQLException e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "Erro no banco de dados: " + e.getMessage());
        }