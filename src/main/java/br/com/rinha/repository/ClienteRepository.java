package br.com.rinha.repository;

import br.com.rinha.config.DatabaseConfig;
import br.com.rinha.model.Cliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Repositório para operações relacionadas a clientes no banco de dados
 * Otimizado para alta concorrência
 */
public class ClienteRepository {
    private static final String SQL_CHECK_CLIENT = "SELECT 1 FROM clientes WHERE id = ?";
    private static final String SQL_GET_CLIENT = "SELECT id, nome, limite, saldo FROM clientes WHERE id = ?";
    private static final String SQL_UPDATE_BALANCE = "UPDATE clientes SET saldo = ? WHERE id = ?";

    // Cache para reduzir consultas ao banco de dados
    private static final ConcurrentHashMap<Integer, Cliente> clienteCache = new ConcurrentHashMap<>();

    // Locks por cliente para evitar race conditions
    private static final ConcurrentHashMap<Integer, Lock> clientLocks = new ConcurrentHashMap<>();

    /**
     * Obtém lock para operações em um cliente específico
     * @param clientId ID do cliente
     * @return Lock para o cliente
     */
    private Lock getClientLock(int clientId) {
        clientLocks.putIfAbsent(clientId, new ReentrantLock());
        return clientLocks.get(clientId);
    }

    /**
     * Verifica se um cliente existe
     * @param clientId ID do cliente
     * @return true se o cliente existe, false caso contrário
     * @throws SQLException em caso de erro no banco de dados
     */
    public boolean clientExists(int clientId) throws SQLException {
        // Primeiro verifica no cache
        if (clienteCache.containsKey(clientId)) {
            return true;
        }

        // Se não estiver no cache, consulta o banco
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_CHECK_CLIENT)) {
            stmt.setInt(1, clientId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Busca um cliente pelo ID (otimizado com cache)
     * @param clientId ID do cliente
     * @return o cliente ou null se não encontrado
     * @throws SQLException em caso de erro no banco de dados
     */
    public Cliente findById(int clientId) throws SQLException {
        // Tenta obter do cache primeiro
        Cliente cachedCliente = clienteCache.get(clientId);
        if (cachedCliente != null) {
            return cachedCliente;
        }

        // Se não estiver no cache, consulta o banco
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_CLIENT)) {
            stmt.setInt(1, clientId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Cliente cliente = new Cliente(
                            rs.getInt("id"),
                            rs.getString("nome"),
                            rs.getInt("limite"),
                            rs.getInt("saldo")
                    );
                    // Armazena no cache
                    clienteCache.put(clientId, cliente);
                    return cliente;
                }
                return null;
            }
        }
    }

    /**
     * Atualiza o saldo de um cliente usando lock por cliente
     * @param clientId ID do cliente
     * @param newBalance novo saldo
     * @throws SQLException em caso de erro no banco de dados
     */
    public void updateBalance(int clientId, int newBalance) throws SQLException {
        Lock lock = getClientLock(clientId);
        lock.lock();
        try {
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_BALANCE)) {
                stmt.setInt(1, newBalance);
                stmt.setInt(2, clientId);
                stmt.executeUpdate();

                // Atualiza o cache se o cliente estiver nele
                Cliente cachedCliente = clienteCache.get(clientId);
                if (cachedCliente != null) {
                    cachedCliente.setSaldo(newBalance);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Realiza uma transação atômica (ideal para ambientes de alta concorrência)
     * Implementa retry com backoff exponencial para aumentar sucesso em alta carga
     * @param clientId ID do cliente
     * @param tipo tipo da transação ("c" para crédito, "d" para débito)
     * @param valor valor da transação
     * @return array com [saldo atual, limite, sucesso (1=sim, 0=não)]
     * @throws SQLException em caso de erro no banco de dados
     */
    public int[] atomicUpdate(int clientId, String tipo, int valor) throws SQLException {
        // Tentativas máximas com backoff exponencial
        int maxRetries = 3;
        int retryCount = 0;
        int baseWaitTimeMs = 10;

        Lock lock = getClientLock(clientId);
        lock.lock();
        try {
            while (retryCount < maxRetries) {
                try {
                    return atomicUpdateInternal(clientId, tipo, valor);
                } catch (SQLException e) {
                    // Verifica se é um erro de concorrência/lock
                    if (e.getSQLState() != null &&
                            (e.getSQLState().startsWith("40") || e.getSQLState().startsWith("23"))) {
                        retryCount++;
                        // Se atingiu o máximo de tentativas, propaga o erro
                        if (retryCount >= maxRetries) {
                            throw e;
                        }

                        // Espera exponencial
                        try {
                            Thread.sleep(baseWaitTimeMs * (1 << retryCount));
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw e;
                        }
                    } else {
                        // Se não for erro de concorrência, propaga imediatamente
                        throw e;
                    }
                }
            }

            // Nunca deve chegar aqui, mas para satisfazer o compilador
            throw new SQLException("Falha após múltiplas tentativas");
        } finally {
            lock.unlock();
        }
    }

    /**
     * Implementação interna da atualização atômica
     */
    private int[] atomicUpdateInternal(int clientId, String tipo, int valor) throws SQLException {
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
                    int newSaldo = rs.getInt("saldo");
                    int limite = rs.getInt("limite");
                    int success = rs.getInt("success");

                    // Atualiza o cache se o cliente estiver nele
                    Cliente cachedCliente = clienteCache.get(clientId);
                    if (cachedCliente != null && success == 1) {
                        cachedCliente.setSaldo(newSaldo);
                    }

                    return new int[] { newSaldo, limite, success };
                }
                throw new SQLException("Falha na atualização do saldo");
            }
        }
    }

    /**
     * Limpa o cache de clientes
     */
    public void clearCache() {
        clienteCache.clear();
    }

    /**
     * Pré-carrega os clientes no cache
     * @throws SQLException em caso de erro no banco de dados
     */
    public void preloadClientCache() throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id, nome, limite, saldo FROM clientes")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Cliente cliente = new Cliente(
                            rs.getInt("id"),
                            rs.getString("nome"),
                            rs.getInt("limite"),
                            rs.getInt("saldo")
                    );
                    clienteCache.put(cliente.getId(), cliente);
                }
            }
        }
    }
}