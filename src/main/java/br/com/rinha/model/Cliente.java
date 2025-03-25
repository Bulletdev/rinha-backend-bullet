package br.com.rinha.model;

/**
 * Entidade que representa um cliente no sistema
 */
public class Cliente {
    private final int id;
    private final String nome;
    private final int limite;
    private int saldo;

    public Cliente(int id, String nome, int limite, int saldo) {
        this.id = id;
        this.nome = nome;
        this.limite = limite;
        this.saldo = saldo;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public int getLimite() {
        return limite;
    }

    public int getSaldo() {
        return saldo;
    }

    public void setSaldo(int saldo) {
        this.saldo = saldo;
    }

    /**
     * Verifica se uma operação de débito é válida com base no limite
     * @param valor valor a ser debitado
     * @return true se o débito é válido, false caso contrário
     */
    public boolean isDebitoValido(int valor) {
        return (saldo - valor) >= -limite;
    }

    /**
     * Realiza uma operação de crédito
     * @param valor valor a ser creditado
     */
    public void creditar(int valor) {
        this.saldo += valor;
    }

    /**
     * Realiza uma operação de débito
     * @param valor valor a ser debitado
     * @return true se o débito foi realizado com sucesso, false caso contrário
     */
    public boolean debitar(int valor) {
        if (isDebitoValido(valor)) {
            this.saldo -= valor;
            return true;
        }
        return false;
    }
}