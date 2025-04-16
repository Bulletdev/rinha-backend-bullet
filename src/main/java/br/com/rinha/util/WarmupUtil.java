package br.com.rinha.util;

import br.com.rinha.config.DatabaseConfig;
import br.com.rinha.repository.ClienteRepository;
import br.com.rinha.repository.TransacaoRepository;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Utilitário para realizar o warmup da aplicação
 * Aquece a JVM, pools de conexão e caches sem alterar o estado do banco
 */
public class WarmupUtil {

    /**
     * Realiza warmup completo da infraestrutura
     */
    public static void performWarmup() {
        System.out.println("Iniciando warmup de infraestrutura...");
        long startTime = System.currentTimeMillis();

        try {
            // Criar instâncias dos repositórios
            ClienteRepository clienteRepository = new ClienteRepository();
            TransacaoRepository transacaoRepository = new TransacaoRepository();

            // Pré-carrega dados importantes na memória
            clienteRepository.preloadClientCache();

            // Aquece o pool de conexões obtendo várias conexões em paralelo
            warmupConnectionPool();

            // Aquece as consultas mais comuns
            warmupQueries(clienteRepository, transacaoRepository);

            // Aquece o JIT compilador executando operações semelhantes às do runtime
            warmupJIT();

            long duration = System.currentTimeMillis() - startTime;
            System.out.println("Warmup de infraestrutura concluído em " + duration + "ms!");
        } catch (Exception e) {
            System.err.println("Erro durante warmup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Aquece o pool de conexões criando e fechando múltiplas conexões
     */
    private static void warmupConnectionPool() throws SQLException, InterruptedException {
        System.out.println("Aquecendo pool de conexões...");

        // Usa threads virtuais para aquecer o pool em paralelo
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < 30; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try (Connection conn = DatabaseConfig.getConnection()) {
                    // Executa uma query simples para estabelecer a conexão
                    try (PreparedStatement stmt = conn.prepareStatement("SELECT 1");
                         ResultSet rs = stmt.executeQuery()) {
                        rs.next(); // Consome o resultado
                    }
                } catch (SQLException e) {
                    System.err.println("Erro durante warmup de conexão: " + e.getMessage());
                }
            }, executor);

            futures.add(future);
        }

        // Aguarda todas as conexões serem processadas
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("Pool de conexões aquecido");
    }

    /**
     * Aquece as consultas mais comuns pré-executando-as
     */
    private static void warmupQueries(ClienteRepository clienteRepository,
                                      TransacaoRepository transacaoRepository) {
        System.out.println("Aquecendo consultas frequentes...");

        try {
            // Executa consultas de clientes e transações para todos os 5 clientes
            for (int i = 1; i <= 5; i++) {
                // Busca cliente para aquecer queries de saldo/limite
                clienteRepository.findById(i);

                // Busca transações para aquecer extrato
                transacaoRepository.getLatestTransactions(i);

                // Simula execução de operações de validação (sem modificar dados)
                try (Connection conn = DatabaseConfig.getConnection()) {
                    // Verifica cliente (query comum)
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "SELECT id, limite, saldo FROM clientes WHERE id = ?")) {
                        stmt.setInt(1, i);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                // apenas lê os dados sem modificá-los
                                rs.getInt("id");
                                rs.getInt("limite");
                                rs.getInt("saldo");
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro durante warmup de queries: " + e.getMessage());
        }

        System.out.println("Consultas frequentes aquecidas");
    }

    /**
     * Aquece o JIT compilador executando operações semelhantes às do runtime
     */
    private static void warmupJIT() {
        System.out.println("Aquecendo JIT compilador...");

        // Simula operações de JSON para aquecer o parser
        for (int i = 0; i < 1000; i++) {
            String jsonStr = "{\"valor\": " + i + ", \"tipo\": \"" +
                    (i % 2 == 0 ? "c" : "d") +
                    "\", \"descricao\": \"warmup\"}";
            try {
                JsonUtil.getObjectMapper().readTree(jsonStr);
            } catch (Exception e) {
            }

            ObjectNode node = JsonUtil.getObjectMapper().createObjectNode();
            node.put("limite", 100000);
            node.put("saldo", i);
            try {
                JsonUtil.getObjectMapper().writeValueAsString(node);
            } catch (Exception e) {
            }
        }

        System.out.println("JIT compilador aquecido");
    }
}