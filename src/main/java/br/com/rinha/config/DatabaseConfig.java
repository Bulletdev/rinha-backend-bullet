package br.com.rinha.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configuração do pool de conexões com o banco de dados
 * Otimizado para alta concorrência
 */
public class DatabaseConfig {
    // Número máximo de conexões para o pool
    private static final int MAX_POOL_SIZE = 40;

    // Número mínimo de conexões ociosas
    private static final int MIN_IDLE = 20;

    // Timeout para obtenção de conexão (ms)
    private static final int CONNECTION_TIMEOUT = 10000;

    private static HikariDataSource dataSource;

    // Contador para monitorar conexões ativas
    private static final AtomicInteger activeConnections = new AtomicInteger(0);

    /**
     * Inicializa o pool de conexões
     */
    public static void initConnectionPool() {
        if (dataSource != null) {
            return;
        }

        try {
            // Database configuration from environment variables with proper defaults

            String dbHost = System.getenv().getOrDefault("DB_HOSTNAME", "db");
            String dbUrl = "jdbc:postgresql://" + dbHost + ":5432/rinha";
            System.out.println("URL de conexão JDBC: " + dbUrl);            String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
            String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "P0rdemacia");

            System.out.println("Configurando conexão com o banco: " + dbUrl);
            System.out.println("Usuário: " + dbUser);

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dbUrl);
            config.setUsername(dbUser);
            config.setPassword(dbPassword);
            config.setDriverClassName("org.postgresql.Driver");

            // Connection pool settings optimized for high concurrency
            config.setMaximumPoolSize(MAX_POOL_SIZE);
            config.setMinimumIdle(MIN_IDLE);
            config.setConnectionTimeout(CONNECTION_TIMEOUT);
            config.setIdleTimeout(300000); // 5 minutos
            config.setMaxLifetime(1800000); // 30 minutos

            // Configure connection test
            config.setConnectionTestQuery("SELECT 1");
            config.setValidationTimeout(5000); // 5 segundos

            // Configurações críticas para alta carga
            config.setAutoCommit(false); // Para controle transacional explícito
            config.setInitializationFailTimeout(30000); // 30 segundos para inicialização
            config.setKeepaliveTime(60000); // 1 minuto
            config.setLeakDetectionThreshold(60000); // 1 minuto

            // Cache de prepared statements
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "500");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "4096");

            // Otimizações PostgreSQL
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");

            // Otimizações adicionais
            config.addDataSourceProperty("tcpKeepAlive", "true");
            config.addDataSourceProperty("socketTimeout", "30"); // 30 segundos

            // Registro de métricas
            config.setMetricRegistry(null); // Remova esta linha se quiser usar métricas
            config.setRegisterMbeans(true);

            // Criação do datasource
            System.out.println("Inicializando pool de conexões com " + MAX_POOL_SIZE + " conexões...");
            dataSource = new HikariDataSource(config);
            System.out.println("Pool de conexões inicializado com sucesso!");

            // Testar a conexão
            try (Connection conn = dataSource.getConnection()) {
                System.out.println("Conexão com o banco estabelecida com sucesso!");
            }
        } catch (SQLException e) {
            System.err.println("Erro crítico ao inicializar pool de conexões: " + e.getMessage());
            e.printStackTrace();

            // Tentar novamente após um tempo
            System.out.println("Tentando reinicializar em 5 segundos...");
            try {
                Thread.sleep(5000);
                initConnectionPool();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Obtém uma conexão do pool com monitoramento usando um Proxy dinâmico
     * @return conexão com o banco de dados
     * @throws SQLException em caso de falha na obtenção da conexão
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            initConnectionPool();
        }

        Connection conn = dataSource.getConnection();
        int current = activeConnections.incrementAndGet();

        // Logs de alerta se estiver chegando perto do limite
        if (current > MAX_POOL_SIZE * 0.8) {
            System.out.println("ALERTA: Uso elevado do pool de conexões: " + current + "/" + MAX_POOL_SIZE);
        }

        // Usa um Proxy para interceptar o método close() sem precisar implementar todos os métodos da interface
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class[] { Connection.class },
                (proxy, method, args) -> {
                    // Intercepta o método close para decrementar o contador
                    if (method.getName().equals("close")) {
                        activeConnections.decrementAndGet();
                    }

                    // Invoca o método original na conexão
                    return method.invoke(conn, args);
                }
        );
    }

    /**
     * Fecha o pool de conexões
     */
    public static void closeConnectionPool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("Pool de conexões fechado");
        }
    }
}