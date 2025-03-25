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

/**
 * Repositório para operações relacionadas a transações no banco de dados
 */
public class TransacaoRepository {
    private static final String SQL_RECORD_TRANSACTION =
            "INSERT INTO transacoes (cliente_id, valor, tipo, descricao) VALUES (?, ?, ?, ?)";

    private static final String SQL_GET_TRANSACTIONS =
            "SELECT valor, tipo, descricao, realizada_em FROM transacoes " +
                    "WHERE cliente_id = ? ORDER BY realizada_em DESC LIMIT 10";

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
     * Executa uma transação de forma atômica usando uma única conexão
     * @param connection Conexão com o banco de dados
     * @param clienteId ID do cliente
     * @param tipo Tipo da transação ("c" para crédito, "d" para débito)
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