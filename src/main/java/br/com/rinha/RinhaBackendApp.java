package br.com.rinha;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import br.com.rinha.config.DatabaseConfig;
import br.com.rinha.handler.ExtratoHandler;
import br.com.rinha.handler.TransacaoHandler;
import br.com.rinha.util.WarmupUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classe principal da aplicação Rinha de Backend
 */
public class RinhaBackendApp {
    private static final Pattern TRANSACTION_PATH_PATTERN = Pattern.compile("/clientes/(\\d+)/transacoes");
    private static final Pattern EXTRACT_PATH_PATTERN = Pattern.compile("/clientes/(\\d+)/extrato");
    private static final int PORT = 9999;

    private static final TransacaoHandler transacaoHandler = new TransacaoHandler();
    private static final ExtratoHandler extratoHandler = new ExtratoHandler();

    /**
     * Método principal de inicialização da aplicação
     * @param args Argumentos de linha de comando (não utilizados)
     * @throws Exception em caso de erro
     */
    public static void main(String[] args) throws Exception {
        // Inicializar o pool de conexões
        DatabaseConfig.initConnectionPool();

        // Realizar warmup da infraestrutura
        System.out.println("Iniciando fase de warmup...");
        WarmupUtil.performWarmup();

        // Criar servidor HTTP com um backlog maior para alta concorrência
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 10000);

        // Configurar rotas
        server.createContext("/", RinhaBackendApp::handleRequest);

        // Adicionar endpoint de health check para facilitar monitoramento
        server.createContext("/health", RinhaBackendApp::handleHealthCheck);

        // Usar virtual threads para processamento de requisições
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        // Configurar hook de encerramento para limpar recursos
        setupShutdownHook();

        // Iniciar o servidor
        server.start();
        System.out.println("Servidor iniciado na porta " + PORT + " usando virtual threads");
    }

    /**
     * Trata todas as requisições HTTP
     * @param exchange Objeto de troca HTTP
     * @throws IOException em caso de erro de I/O
     */
    private static void handleRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            // Tratar transações
            Matcher transactionMatcher = TRANSACTION_PATH_PATTERN.matcher(path);
            if (method.equals("POST") && transactionMatcher.matches()) {
                int clientId = Integer.parseInt(transactionMatcher.group(1));
                transacaoHandler.handle(exchange, clientId);
                return;
            }

            // Tratar extratos
            Matcher extractMatcher = EXTRACT_PATH_PATTERN.matcher(path);
            if (method.equals("GET") && extractMatcher.matches()) {
                int clientId = Integer.parseInt(extractMatcher.group(1));
                extratoHandler.handle(exchange, clientId);
                return;
            }

            // Tratar 404 Not Found
            sendResponse(exchange, 404, "Rota não encontrada");
        } catch (NumberFormatException e) {
            // ID de cliente inválido
            sendResponse(exchange, 404, "ID de cliente inválido");
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "Erro interno do servidor: " + e.getMessage());
        } finally {
            exchange.close();
        }
    }

    /**
     * Endpoint de health check para monitoramento
     * @param exchange Objeto de troca HTTP
     * @throws IOException em caso de erro de I/O
     */
    private static void handleHealthCheck(HttpExchange exchange) throws IOException {
        try {
            // Verificar se o banco de dados está acessível
            boolean dbHealthy = false;
            try (var conn = DatabaseConfig.getConnection()) {
                try (var stmt = conn.prepareStatement("SELECT 1")) {
                    try (var rs = stmt.executeQuery()) {
                        dbHealthy = rs.next();
                    }
                }
            } catch (Exception e) {
                // Conexão com banco falhou
                dbHealthy = false;
            }

            if (dbHealthy) {
                sendResponse(exchange, 200, "OK");
            } else {
                sendResponse(exchange, 503, "Database connection failed");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "Health check failed: " + e.getMessage());
        }
    }

    /**
     * Método auxiliar para enviar respostas HTTP
     * @param exchange Objeto de troca HTTP
     * @param statusCode Código de status HTTP
     * @param message Mensagem de resposta
     * @throws IOException em caso de erro de I/O
     */
    private static void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        byte[] responseBytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    /**
     * Configura um hook de encerramento para limpar recursos quando a aplicação for terminada
     */
    private static void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Encerrando servidor e pool de conexões...");
            DatabaseConfig.closeConnectionPool();
        }));
    }
}