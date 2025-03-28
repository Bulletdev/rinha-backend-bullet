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

import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken;
import org.openapitools.client.ApiCallback;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.ApiResponse;
import org.openapitools.client.Configuration;
import org.openapitools.client.Pair;

import com.google.gson.reflect.TypeToken;


import org.openapitools.client.model.Extrato;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtratosApi {
    private ApiClient localVarApiClient;
    private int localHostIndex;
    private String localCustomBaseUrl;

    public ExtratosApi() {
        this(Configuration.getDefaultApiClient());
    }

    public ExtratosApi(ApiClient apiClient) {
        this.localVarApiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return localVarApiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.localVarApiClient = apiClient;
    }

    public int getHostIndex() {
        return localHostIndex;
    }

    public void setHostIndex(int hostIndex) {
        this.localHostIndex = hostIndex;
    }

    public String getCustomBaseUrl() {
        return localCustomBaseUrl;
    }

    public void setCustomBaseUrl(String customBaseUrl) {
        this.localCustomBaseUrl = customBaseUrl;
    }

    /**
     * Build call for clientesIdExtratoGet
     * @param id ID do cliente (required)
     * @param _callback Callback for upload/download progress
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Extrato obtido com sucesso </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Cliente não encontrado </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call clientesIdExtratoGetCall(Integer id, final ApiCallback _callback) throws ApiException {
        String basePath = null;
        // Operation Servers
        String[] localBasePaths = new String[] {  };

        // Determine Base Path to Use
        if (localCustomBaseUrl != null){
            basePath = localCustomBaseUrl;
        } else if ( localBasePaths.length > 0 ) {
            basePath = localBasePaths[localHostIndex];
        } else {
            basePath = null;
        }

        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/clientes/{id}/extrato"
            .replace("{" + "id" + "}", localVarApiClient.escapeString(id.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
        };
        final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] {  };
        return localVarApiClient.buildCall(basePath, localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    @SuppressWarnings("rawtypes")
    private okhttp3.Call clientesIdExtratoGetValidateBeforeCall(Integer id, final ApiCallback _callback) throws ApiException {
        // verify the required parameter 'id' is set
        if (id == null) {
            throw new ApiException("Missing the required parameter 'id' when calling clientesIdExtratoGet(Async)");
        }

        return clientesIdExtratoGetCall(id, _callback);

    }

    /**
     * Obter extrato do cliente
     * Retorna o saldo atual e as últimas transações realizadas pelo cliente (até 10). As transações são ordenadas por data, da mais recente para a mais antiga. 
     * @param id ID do cliente (required)
     * @return Extrato
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Extrato obtido com sucesso </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Cliente não encontrado </td><td>  -  </td></tr>
     </table>
     */
    public Extrato clientesIdExtratoGet(Integer id) throws ApiException {
        ApiResponse<Extrato> localVarResp = clientesIdExtratoGetWithHttpInfo(id);
        return localVarResp.getData();
    }

    /**
     * Obter extrato do cliente
     * Retorna o saldo atual e as últimas transações realizadas pelo cliente (até 10). As transações são ordenadas por data, da mais recente para a mais antiga.
     *
     * @param id ID do cliente (required)
     * @return ApiResponse&lt;Extrato&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details <table summary="Response Details" border="1">
     * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
     * <tr><td> 200 </td><td> Extrato obtido com sucesso </td><td>  -  </td></tr>
     * <tr><td> 404 </td><td> Cliente não encontrado </td><td>  -  </td></tr>
     * </table>
     */
    public ApiResponse<T> clientesIdExtratoGetWithHttpInfo(Integer id) throws ApiException {
        okhttp3.Call localVarCall = clientesIdExtratoGetValidateBeforeCall(id, null);
        Type localVarReturnType = new TypeToken<Extrato>(){}.getType();
        return localVarApiClient.execute(localVarCall, localVarReturnType);
    }

    /**
     * Obter extrato do cliente (asynchronously)
     * Retorna o saldo atual e as últimas transações realizadas pelo cliente (até 10). As transações são ordenadas por data, da mais recente para a mais antiga. 
     * @param id ID do cliente (required)
     * @param _callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Extrato obtido com sucesso </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Cliente não encontrado </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call clientesIdExtratoGetAsync(Integer id, final ApiCallback<Extrato> _callback) throws ApiException {

        okhttp3.Call localVarCall = clientesIdExtratoGetValidateBeforeCall(id, _callback);
        Type localVarReturnType = new TypeToken<Extrato>(){}.getType();
        localVarApiClient.executeAsync(localVarCall, localVarReturnType, _callback);
        return localVarCall;
    }
}
