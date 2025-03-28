/*
 * Rinha de Backend 2024/Q1 API
 * API para gerenciamento de transações financeiras e consulta de extratos. Desenvolvida com Java 21 utilizando Project Loom (Virtual Threads) para alta concorrência. 
 *
 * The version of the OpenAPI document: 1.0.0
 * Contact: contato@michaelbullet.com
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package org.openapitools.client.api;

import org.openapitools.client.ApiException;
import org.openapitools.client.model.Extrato;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API tests for ExtratosApi
 */
@Disabled
public class ExtratosApiTest {

    private final org.openapitools.client.api.ExtratosApi api = new org.openapitools.client.api.ExtratosApi();

    /**
     * Obter extrato do cliente
     *
     * Retorna o saldo atual e as últimas transações realizadas pelo cliente (até 10). As transações são ordenadas por data, da mais recente para a mais antiga. 
     *
     * @throws ApiException if the Api call fails
     */
    @Test
    public void clientesIdExtratoGetTest() throws ApiException {
        Integer id = null;
        Extrato response = api.clientesIdExtratoGet(id);
        // TODO: test validations
    }

}
