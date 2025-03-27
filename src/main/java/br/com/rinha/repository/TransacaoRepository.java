package br.com.rinha.repository;

import br.com.rinha.config.DatabaseConfig;
import br.com.rinha.model.Transacao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Repositório para operações relacionadas a transações no banco de dados
 * Otimizado para alta concorrência com suporte a salvamento assíncrono
 */
public class TransacaoRepository {
    private static final String SQL_RECORD_TRANSACTION =
            "INSERT INTO transacoes (cliente_id, valor, tipo, descricao) VALUES (?, ?, ?, ?)";

    private static final String SQL_GET_TRANSACTIONS =
            "SELECT valor, tipo, descricao, realizada_em FROM transacoes " +
                    "WHERE cliente_id = ? ORDER BY realizada_em DESC LIMIT 10";

    // Fila para armazenar transações pendentes para salvamento assíncrono
    private final ConcurrentLinkedQueue<Transacao> transactionQueue = new ConcurrentLinkedQueue<>();

    // Executor para processamento assíncrono em batch
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * Construtor que inicia o processamento assíncrono de transações
     */
    public TransacaoRepository() {
        // Processa a fila de transações a cada 100ms
        scheduler.scheduleWithFixedDelay(this::processBatchTransactions, 100, 100, TimeUnit.MILLISECONDS);

        // Registra shutdown hook para garantir que transações pendentes sejam processadas
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdown();
            try {
                // Aguarda até 5 segundos para processar transações pendentes
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }

                // Processa manualmente quaisquer transações restantes
                processBatchTransactions();
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }));
    }

    /**
     * Registra uma nova transação no banco de dados
     * @param transacao transação a ser registrada
     * @throws SQLException em caso de erro no banco de dados
     */
    public void save(Transacao transacao) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_RECORD_TRANSACTION)) {
            stmt.setInt(1, transacao.getClienteId());
            stmt.setInt(2, transacao.getValor());
            stmt.setString(3, transacao.getTipo());
            stmt.setString(4, transacao.getDescricao());
            stmt.executeUpdate();
        }
    }

    /**
     * Adiciona uma transação para salvamento assíncrono
     * Este método retorna imediatamente sem bloquear
     * @param transacao transação a ser salva assincronamente
     */
    public void saveAsync(Transacao transacao) {
        transactionQueue.add(transacao);
    }

    /**
     * Processa um lote de transações da fila
     * Este método é chamado periodicamente pelo scheduler
     */
    private void processBatchTransactions() {
        if (transactionQueue.isEmpty()) {
            return;
        }

        List<Transacao> batch = new ArrayList<>();
        // Coleta até 100 transações para processar em lote
        for (int i = 0; i < 100 && !transactionQueue.isEmpty(); i++) {
            Transacao transacao = transactionQueue.poll();
            if (transacao != null) {
                batch.add(transacao);
            }
        }

        if (batch.isEmpty()) {
            return;
        }

        // Processa o lote em uma única conexão
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(SQL_RECORD_TRANSACTION)) {
                for (Transacao transacao : batch) {
                    stmt.setInt(1, transacao.getClienteId());
                    stmt.setInt(2, transacao.getValor());
                    stmt.setString(3, transacao.getTipo());
                    stmt.setString(4, transacao.getDescricao());
                    stmt.addBatch();
                }

                stmt.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Erro ao processar lote de transações: " + e.getMessage());
                e.printStackTrace();

                // Recoloca as transações na fila para tentar novamente
                transactionQueue.addAll(batch);
            }
        } catch (SQLException e) {
            System.err.println("Erro de conexão ao processar lote: " + e.getMessage());
            e.printStackTrace();

            // Recoloca as transações na fila para tentar novamente
            transactionQueue.addAll(batch);
        }
    }

    /**
     * Obtém as últimas transações de um cliente
     * @param clienteId ID do cliente
     * @return Lista de transações
     * @throws SQLException em caso de erro no banco de dados
     */
    public List<Transacao> getLatestTransactions(int clienteId) throws SQLException {
        List<Transacao> transactions = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_TRANSACTIONS)) {
            stmt.setInt(1, clienteId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Transacao transaction = new Transacao(
                            clienteId,
                            rs.getInt("valor"),
                            rs.getString("tipo"),
                            rs.getString("descricao"),
                            convertTimestamp(rs.getTimestamp("realizada_em"))
                    );
                    transactions.add(transaction);
                }
            }
        }
        return transactions;
    }

    /**
     * Converte um Timestamp SQL para ZonedDateTime
     * @param timestamp O timestamp do banco de dados
     * @return O ZonedDateTime correspondente
     */
    private ZonedDateTime convertTimestamp(Timestamp timestamp) {
        return ZonedDateTime.of(timestamp.toLocalDateTime(), ZoneId.of("UTC"));
    }

    /**
     * Executa uma transação com uma conexão específica
     * @param connection Conexão com o banco de dados
     * @param clienteId ID do cliente
     * @param tipo Tipo da transação
     * @param valor Valor da transação
     * @param descricao Descrição da transação
     * @throws SQLException em caso de erro no banco de dados
     */
    public void saveWithConnection(Connection connection, int clienteId, String tipo, int valor, String descricao)
            throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(SQL_RECORD_TRANSACTION)) {
            stmt.setInt(1, clienteId);
            stmt.setInt(2, valor);
            stmt.setString(3, tipo);
            stmt.setString(4, descricao);
            stmt.executeUpdate();
        }
    }
}