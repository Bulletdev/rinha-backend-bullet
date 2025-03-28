# ExtratosApi

All URIs are relative to *http://localhost:9999*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**clientesIdExtratoGet**](ExtratosApi.md#clientesIdExtratoGet) | **GET** /clientes/{id}/extrato | Obter extrato do cliente |


<a id="clientesIdExtratoGet"></a>
# **clientesIdExtratoGet**
> Extrato clientesIdExtratoGet(id)

Obter extrato do cliente

Retorna o saldo atual e as últimas transações realizadas pelo cliente (até 10). As transações são ordenadas por data, da mais recente para a mais antiga. 

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.ExtratosApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:9999");

    ExtratosApi apiInstance = new ExtratosApi(defaultClient);
    Integer id = 1; // Integer | ID do cliente
    try {
      Extrato result = apiInstance.clientesIdExtratoGet(id);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ExtratosApi#clientesIdExtratoGet");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters

| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **id** | **Integer**| ID do cliente | |

### Return type

[**Extrato**](Extrato.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Extrato obtido com sucesso |  -  |
| **404** | Cliente não encontrado |  -  |

