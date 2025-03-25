-- Create clients table - optimized structure for high concurrency
CREATE TABLE IF NOT EXISTS clientes (
                                        id INTEGER PRIMARY KEY,
                                        nome VARCHAR(100) NOT NULL,
                                        limite INTEGER NOT NULL,
                                        saldo INTEGER NOT NULL DEFAULT 0
);

-- Create transactions table with index for querying the latest transactions
CREATE TABLE IF NOT EXISTS transacoes (
                                          id SERIAL,
                                          cliente_id INTEGER NOT NULL,
                                          valor INTEGER NOT NULL,
                                          tipo CHAR(1) NOT NULL,
                                          descricao VARCHAR(10) NOT NULL,
                                          realizada_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          PRIMARY KEY (id),
                                          CONSTRAINT fk_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
);

-- Create index for faster transaction retrieval
CREATE INDEX IF NOT EXISTS idx_transacoes_cliente_data ON transacoes (cliente_id, realizada_em DESC);

-- Create specialized index for the transaction operations for faster transaction lookups
CREATE INDEX IF NOT EXISTS idx_transacoes_cliente ON transacoes (cliente_id);

-- Insert initial data for the 5 clients - using direct insert for better performance
INSERT INTO clientes (id, nome, limite, saldo)
VALUES
    (1, 'o barato sai caro', 100000, 0),
    (2, 'zan corp ltda', 80000, 0),
    (3, 'les cruders', 1000000, 0),
    (4, 'padaria joia de cocaia', 10000000, 0),
    (5, 'kid mais', 500000, 0)
ON CONFLICT (id) DO NOTHING;

-- Optimize tables for better performance
ANALYZE clientes;
ANALYZE transacoes;