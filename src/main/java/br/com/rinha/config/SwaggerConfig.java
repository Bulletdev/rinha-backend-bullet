package br.com.rinha.config;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuração do Swagger UI para documentação da API
 */
public class SwaggerConfig {

    // Mapa de recursos Swagger
    private static final Map<String, String> SWAGGER_RESOURCES = new HashMap<>();
    private static final String SWAGGER_HTML = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>Rinha de Backend API - Swagger UI</title>
                <link rel="stylesheet" type="text/css" href="/swagger-ui/swagger-ui.css" />
                <link rel="icon" type="image/png" href="/swagger-ui/favicon-32x32.png" sizes="32x32" />
                <link rel="icon" type="image/png" href="/swagger-ui/favicon-16x16.png" sizes="16x16" />
                <style>
                    html { box-sizing: border-box; overflow: -moz-scrollbars-vertical; overflow-y: scroll; }
                    *, *:before, *:after { box-sizing: inherit; }
                    body { margin: 0; background: #fafafa; }
                </style>
            </head>
            <body>
                <div id="swagger-ui"></div>
                <script src="/swagger-ui/swagger-ui-bundle.js" charset="UTF-8"></script>
                <script src="/swagger-ui/swagger-ui-standalone-preset.js" charset="UTF-8"></script>
                <script>
                window.onload = function() {
                    window.ui = SwaggerUIBundle({
                        url: "/api-docs",
                        dom_id: '#swagger-ui',
                        deepLinking: true,
                        presets: [
                            SwaggerUIBundle.presets.apis,
                            SwaggerUIStandalonePreset
                        ],
                        plugins: [
                            SwaggerUIBundle.plugins.DownloadUrl
                        ],
                        layout: "StandaloneLayout"
                    });
                };
                </script>
            </body>
            </html>
            """;

    // Inicializa os recursos do Swagger
    static {
         SWAGGER_RESOURCES.put("/api-docs", getSwaggerYaml());


    }

    /**
     * Registra os endpoints do Swagger no servidor HTTP
     * @param server Servidor HTTP
     */
    public static void registerSwaggerEndpoints(HttpServer server) {
        // Endpoint para a interface Swagger UI
        server.createContext("/swagger", new SwaggerUIHandler());

        // Endpoint para o arquivo de especificação OpenAPI
        server.createContext("/api-docs", new ApiDocsHandler());

        System.out.println("Swagger UI disponível em: http://localhost:8080/swagger");
    }

    /**
     * Handler para a interface Swagger UI
     */
    static class SwaggerUIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Retorna a página HTML do Swagger UI
            byte[] response = SWAGGER_HTML.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }
    }

    /**
     * Handler para o arquivo de especificação OpenAPI
     */
    static class ApiDocsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Retorna o conteúdo do arquivo swagger.yaml
            String yamlContent = getSwaggerYaml();
            byte[] response = yamlContent.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set("Content-Type", "application/yaml");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }
    }

    /**
     * Obtém o conteúdo do arquivo swagger.yaml
     * @return Conteúdo do arquivo swagger.yaml como String
     */
    private static String getSwaggerYaml() {
        // Primeiro, tentar ler do sistema de arquivos (para desenvolvimento)
        try {
            Path path = Paths.get("swagger.yaml");
            if (Files.exists(path)) {
                return Files.readString(path, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            System.err.println("Não foi possível ler o arquivo swagger.yaml do sistema de arquivos: " + e.getMessage());
        }

        // Depois, tentar ler do classpath (para produção)
        try (InputStream is = SwaggerConfig.class.getClassLoader().getResourceAsStream("swagger.yaml")) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            System.err.println("Não foi possível ler o arquivo swagger.yaml do classpath: " + e.getMessage());
        }

        // Se não conseguir ler de nenhum lugar, retornar um conteúdo padrão
        return """
               openapi: 3.0.0
               info:
                 title: Rinha de Backend 2024/Q1 API
                 version: 1.0.0
               paths:
                 /clientes/{id}/transacoes:
                   post:
                     summary: Criar nova transação
                 /clientes/{id}/extrato:
                   get:
                     summary: Obter extrato do cliente
               """;
    }
}