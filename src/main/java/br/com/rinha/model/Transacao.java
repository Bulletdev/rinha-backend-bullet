package br.com.rinha.model;

import java.time.ZonedDateTime;

/**
 * Entidade que representa uma transação no sistema
 */
public class Transacao {
    private final int clienteId;
    private final int valor;
    private final String tipo;
    private final String descricao;
    private final ZonedDateTime realizadaEm;

    public Transacao(int clienteId, int valor, String tipo, String descricao) {
        this(clienteId, valor, tipo, descricao, ZonedDateTime.now());
    }

    public Transacao(int clienteId, int valor, String tipo, String descricao, ZonedDateTime realizadaEm) {
        this.clienteId = clienteId;
        this.valor = valor;
        this.tipo = tipo;
        this.descricao = descricao;
        this.realizadaEm = realizadaEm;
    }

    public int getClienteId() {
        return clienteId;
    }

    public int getValor() {
        return valor;
    }

    public String getTipo() {
        return tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public ZonedDateTime getRealizadaEm() {
        return realizadaEm;
    }

    /**
     * Valida se os dados da transação estão corretos
     * @return true se a transação é válida, false caso contrário
     */
    public boolean isValid() {
        // Valor precisa ser positivo
        if (valor <= 0) {
            return false;
        }

        // Tipo precisa ser "c" ou "d"
        if (!"c".equals(tipo) && !"d".equals(tipo)) {
            return false;
        }

        // Descrição precisa ter entre 1 e 10 caracteres
        return descricao != null && descricao.length() >= 1 && descricao.length() <= 10;
    }
}