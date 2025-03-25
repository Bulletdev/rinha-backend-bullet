package br.com.rinha.repository;

import br.com.rinha.config.DatabaseConfig;
import br.com.rinha.model.Cliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Repositório para operações relacionadas a clientes no banco de dados
 */
public class ClienteRepository {
    private static final String SQL_CHECK_CLIENT = "SELECT 1 FROM clientes WHERE id = ?";
    private static final String SQL_GET_CLIENT = "SELECT id, nome, limite, saldo FROM clientes WHERE id = ?";
    private static final String SQL_UPDATE_BALANCE = "UPDATE clientes SET saldo = ? WHERE id = ?";

    /**
     * Verifica se um cliente existe
     * @param clientId ID do cliente
     * @return true se o cliente existe, false caso contrário
     * @throws SQLException em caso de erro no banco de dados
     */
    public boolean clientExists(int clientId) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_CHECK_CLIENT)) {
            stmt.setInt(1, clientId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Busca um cliente pelo ID
     * @param clientId ID do cliente
     * @return o cliente ou null se não encontrado
     * @throws SQLException em caso de erro no banco de dados
     */
    public Cliente findById(int clientId) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_CLIENT)) {
            stmt.setInt(1, clientId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Cliente(
                            rs.getInt("id"),
                            rs.getString("nome"),
                            rs.getInt("limite"),
                            rs.getInt("saldo")
                    );
                }
                return null;
            }
        }
    }

    /**
     * Atualiza o saldo de um cliente
     * @param clientId ID do cliente
     * @param newBalance novo saldo
     * @throws SQLException em caso de erro no banco de dados
     */
    public void updateBalance(int clientId, int newBalance) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_BALANCE)) {
            stmt.setInt(1, newBalance);
            stmt.setInt(2, clientId);
            stmt.executeUpdate();
        }
    }

    /**
     * Realiza uma transação atômica (ideal para ambientes de alta concorrência)
     * @param clientId ID do cliente
     * @param tipo tipo da transação ("c" para crédito, "d" para débito)
     * @param valor valor da transação
     * @return array com [saldo atual, limite, sucesso (1=sim, 0=não)]
     * @throws SQLException em caso de erro no banco de dados
     */
    public int[] atomicUpdate(int clientId, String tipo, int valor) throws SQLException {
        final String SQL_ATOMIC_UPDATE =
                "UPDATE clientes SET saldo = " +
                        "CASE " +
                        "  WHEN (? = 'c') THEN saldo + ? " +
                        "  WHEN (? = 'd' AND (saldo - ?) >= -limite) THEN saldo - ? " +
                        "  ELSE saldo " +
                        "END " +
                        "WHERE id = ? RETURNING saldo, limite, " +
                        "CASE " +
                        "  WHEN (? = 'd' AND (saldo - ?) < -limite) THEN 0 " +
                        "  ELSE 1 " +
                        "END as success";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_ATOMIC_UPDATE)) {
            stmt.setString(1, tipo);
            stmt.setInt(2, valor);
            stmt.setString(3, tipo);
            stmt.setInt(4, valor);
            stmt.setInt(5, valor);
            stmt.setInt(6, clientId);
            stmt.setString(7, tipo);
            stmt.setInt(8, valor);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new int[] {
                            rs.getInt("saldo"),
                            rs.getInt("limite"),
                            rs.getInt("success")
                    };
                }
                throw new SQLException("Falha na atualização do saldo");
            }
        }
    }
}