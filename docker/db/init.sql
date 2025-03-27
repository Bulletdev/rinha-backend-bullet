-- Configurações de performance para o PostgreSQL
ALTER SYSTEM SET max_connections = '200';
ALTER SYSTEM SET shared_buffers = '256MB';
ALTER SYSTEM SET effective_cache_size = '300MB';
ALTER SYSTEM SET maintenance_work_mem = '128MB';
ALTER SYSTEM SET work_mem = '12MB';
ALTER SYSTEM SET wal_buffers = '16MB';
ALTER SYSTEM SET checkpoint_completion_target = '0.9';
ALTER SYSTEM SET random_page_cost = '1.1';
ALTER SYSTEM SET effective_io_concurrency = '200';
ALTER SYSTEM SET max_worker_processes = '8';
ALTER SYSTEM SET max_parallel_workers_per_gather = '4';
ALTER SYSTEM SET max_parallel_workers = '8';
ALTER SYSTEM SET synchronous_commit = 'off';

-- Cria tabela de clientes otimizada para alta concorrência
CREATE TABLE IF NOT EXISTS clientes (
                                        id INTEGER PRIMARY KEY,
                                        nome VARCHAR(100) NOT NULL,
                                        limite INTEGER NOT NULL,
                                        saldo INTEGER NOT NULL DEFAULT 0
) WITH (fillfactor=70);

-- Cria tabela de transações com particionamento por cliente
CREATE TABLE IF NOT EXISTS transacoes (
                                          id SERIAL,
                                          cliente_id INTEGER NOT NULL,
                                          valor INTEGER NOT NULL,
                                          tipo CHAR(1) NOT NULL,
                                          descricao VARCHAR(10) NOT NULL,
                                          realizada_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          PRIMARY KEY (id, cliente_id)
) PARTITION BY HASH (cliente_id);

-- Cria partições para distribuir a carga
CREATE TABLE transacoes_part_1 PARTITION OF transacoes
    FOR VALUES WITH (modulus 5, remainder 0);
CREATE TABLE transacoes_part_2 PARTITION OF transacoes
    FOR VALUES WITH (modulus 5, remainder 1);
CREATE TABLE transacoes_part_3 PARTITION OF transacoes
    FOR VALUES WITH (modulus 5, remainder 2);
CREATE TABLE transacoes_part_4 PARTITION OF transacoes
    FOR VALUES WITH (modulus 5, remainder 3);
CREATE TABLE transacoes_part_5 PARTITION OF transacoes
    FOR VALUES WITH (modulus 5, remainder 4);

-- Cria índices de alta performance
CREATE INDEX IF NOT EXISTS idx_transacoes_cliente_data ON transacoes (cliente_id, realizada_em DESC);
CREATE INDEX IF NOT EXISTS idx_transacoes_cliente ON transacoes (cliente_id);

-- Adiciona constraint de chave estrangeira
ALTER TABLE transacoes ADD CONSTRAINT fk_cliente
    FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE;

-- Adiciona regra de verificação para tipo
ALTER TABLE transacoes ADD CONSTRAINT check_tipo
    CHECK (tipo IN ('c', 'd'));

-- Adiciona regra de verificação para valor
ALTER TABLE transacoes ADD CONSTRAINT check_valor
    CHECK (valor > 0);

-- Adiciona regra de verificação para saldo e limite
ALTER TABLE clientes ADD CONSTRAINT check_saldo_limite
    CHECK (saldo >= -limite);

-- Insere dados iniciais dos clientes
INSERT INTO clientes (id, nome, limite, saldo)
VALUES
    (1, 'o barato sai caro', 100000, 0),
    (2, 'zan corp ltda', 80000, 0),
    (3, 'les cruders', 1000000, 0),
    (4, 'padaria joia de cocaia', 10000000, 0),
    (5, 'kid mais', 500000, 0)
ON CONFLICT (id) DO NOTHING;

-- Cria função para validar transações
CREATE OR REPLACE FUNCTION fn_validar_transacao(
    p_cliente_id INTEGER,
    p_valor INTEGER,
    p_tipo CHAR(1)
) RETURNS BOOLEAN AS $$
DECLARE
    v_saldo INTEGER;
    v_limite INTEGER;
BEGIN
    -- Obtém saldo e limite atuais
    SELECT saldo, limite INTO v_saldo, v_limite
    FROM clientes
    WHERE id = p_cliente_id
        FOR UPDATE; -- Lock para evitar race conditions

    -- Valida a transação
    IF p_tipo = 'd' THEN
        -- Verifica se há saldo disponível para débito
        RETURN (v_saldo - p_valor) >= -v_limite;
    ELSE
        -- Crédito sempre é válido
        RETURN TRUE;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Cria procedure para processar transação
CREATE OR REPLACE PROCEDURE pr_processar_transacao(
    p_cliente_id INTEGER,
    p_valor INTEGER,
    p_tipo CHAR(1),
    p_descricao VARCHAR(10),
    INOUT p_novo_saldo INTEGER,
    INOUT p_limite INTEGER,
    INOUT p_sucesso BOOLEAN
) AS $$
DECLARE
    v_saldo_atual INTEGER;
BEGIN
    -- Inicia com falha
    p_sucesso := FALSE;

    -- Obtém saldo e limite atuais
    SELECT saldo, limite INTO v_saldo_atual, p_limite
    FROM clientes
    WHERE id = p_cliente_id
        FOR UPDATE; -- Lock para evitar race conditions

    -- Calcula novo saldo
    IF p_tipo = 'c' THEN
        p_novo_saldo := v_saldo_atual + p_valor;
    ELSE
        p_novo_saldo := v_saldo_atual - p_valor;

        -- Valida limite
        IF p_novo_saldo < -p_limite THEN
            RETURN; -- Sai sem fazer alterações
        END IF;
    END IF;

    -- Atualiza saldo
    UPDATE clientes
    SET saldo = p_novo_saldo
    WHERE id = p_cliente_id;

    -- Registra transação
    INSERT INTO transacoes (cliente_id, valor, tipo, descricao)
    VALUES (p_cliente_id, p_valor, p_tipo, p_descricao);

    -- Marca como sucesso
    p_sucesso := TRUE;
END;
$$ LANGUAGE plpgsql;

-- Analisar tabelas para otimizar consultas
ANALYZE clientes;
ANALYZE transacoes;