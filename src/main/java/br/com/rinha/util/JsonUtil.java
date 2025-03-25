package br.com.rinha.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import br.com.rinha.model.Cliente;
import br.com.rinha.model.Transacao;

import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Utilitário para manipulação de JSON
 */
public class JsonUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");

    /**
     * Cria um JSON de resposta para uma transação
     * @param limite Limite do cliente
     * @param saldo Saldo atual do cliente
     * @return ObjectNode com os dados formatados
     */
    public static ObjectNode createTransactionResponse(int limite, int saldo) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("limite", limite);
        response.put("saldo", saldo);
        return response;
    }

    /**
     * Cria um JSON de resposta para um extrato
     * @param cliente Cliente do extrato
     * @param transacoes Lista de transações
     * @return ObjectNode com os dados formatados
     */
    public static ObjectNode createExtractResponse(Cliente cliente, List<Transacao> transacoes) {
        ObjectNode response = objectMapper.createObjectNode();

        // Add balance information
        ObjectNode balanceInfo = response.putObject("saldo");
        balanceInfo.put("total", cliente.getSaldo());
        balanceInfo.put("limite", cliente.getLimite());

        // Format current datetime in ISO format
        ZonedDateTime now = ZonedDateTime.now();
        balanceInfo.put("data_extrato", formatDateTime(now));

        // Add transactions
        ArrayNode transactionsArray = response.putArray("ultimas_transacoes");
        for (Transacao t : transacoes) {
            ObjectNode transactionNode = transactionsArray.addObject();
            transactionNode.put("valor", t.getValor());
            transactionNode.put("tipo", t.getTipo());
            transactionNode.put("descricao", t.getDescricao());
            transactionNode.put("realizada_em", formatDateTime(t.getRealizadaEm()));
        }

        return response;
    }

    /**
     * Formata um ZonedDateTime para o formato ISO
     * @param dateTime Data e hora a ser formatada
     * @return String formatada
     */
    public static String formatDateTime(ZonedDateTime dateTime) {
        return dateTime.format(ISO_FORMATTER);
    }

    /**
     * Obtém a instância do ObjectMapper
     * @return ObjectMapper
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}