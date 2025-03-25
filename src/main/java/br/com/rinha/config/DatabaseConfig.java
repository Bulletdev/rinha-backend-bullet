package br.com.rinha.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Configuração do pool de conexões com o banco de dados
 */
public class DatabaseConfig {
    private static HikariDataSource dataSource;

    /**
     * Inicializa o pool de conexões
     */
    public static void initConnectionPool() {
        if (dataSource != null) {
            return;
        }

        // Database configuration from environment variables or defaults
        String dbHost = System.getenv().getOrDefault("DB_HOSTNAME", "localhost");
        String dbUrl = "jdbc:postgresql://" + dbHost + ":5432/rinha";
        String dbUser = System.getenv().getOrDefault("DB_USER", "admin");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "123");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setDriverClassName("org.postgresql.Driver");

        // Connection pool settings optimized for high concurrency
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(10);
        config.setConnectionTimeout(10000); // 10 seconds
        config.setIdleTimeout(600000); // 10 minutes
        config.setMaxLifetime(1800000); // 30 minutes
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        // Optimize for PostgreSQL
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        // Create datasource
        dataSource = new HikariDataSource(config);
        System.out.println("Connection pool initialized");
    }

    /**
     * Obtém uma conexão do pool
     * @return conexão com o banco de dados
     * @throws SQLException em caso de falha na obtenção da conexão
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            initConnectionPool();
        }
        return dataSource.getConnection();
    }

    /**
     * Fecha o pool de conexões
     */
    public static void closeConnectionPool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("Connection pool closed");
        }
    }
}