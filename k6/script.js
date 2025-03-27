import http from 'k6/http';
import { check, sleep } from 'k6';

// Configuração do teste
export const options = {
    stages: [
        { duration: '10s', target: 10 },
        { duration: '30s', target: 100 },
        { duration: '1m', target: 100 },
        { duration: '30s', target: 0 },
    ],
};

// Lista de IDs válidos de clientes
const clientIds = [1, 2, 3, 4, 5];

// Lista de transações para testar
const transactions = [
    { tipo: 'c', valor: 1000, descricao: 'salario' },
    { tipo: 'd', valor: 100, descricao: 'compra' },
    { tipo: 'c', valor: 500, descricao: 'estorno' },
    { tipo: 'd', valor: 750, descricao: 'aluguel' },
    { tipo: 'd', valor: 90, descricao: 'conta luz' },
];

// URL base da API
const BASE_URL = 'http://localhost:9999';

// Função principal de teste
export default function () {
    // Escolhe um cliente aleatório
    const clientId = clientIds[Math.floor(Math.random() * clientIds.length)];

    // Escolhe uma transação aleatória
    const transaction = transactions[Math.floor(Math.random() * transactions.length)];

    // 70% das vezes faz uma transação, 30% consulta extrato
    if (Math.random() < 0.7) {
        // Teste POST /clientes/{id}/transacoes
        const transactionUrl = `${BASE_URL}/clientes/${clientId}/transacoes`;
        const payload = JSON.stringify(transaction);

        const transactionResponse = http.post(transactionUrl, payload, {
            headers: { 'Content-Type': 'application/json' }
        });

        check(transactionResponse, {
            'transaction status is 200': (r) => r.status === 200
        });

    } else {
        // Teste GET /clientes/{id}/extrato
        const extractUrl = `${BASE_URL}/clientes/${clientId}/extrato`;
        const extractResponse = http.get(extractUrl);

        check(extractResponse, {
            'extract status is 200': (r) => r.status === 200
        });
    }

    // Pequena pausa entre requisições
    sleep(0.1);
}