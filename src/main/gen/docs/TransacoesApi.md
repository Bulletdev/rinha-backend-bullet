# TransacoesApi

All URIs are relative to *http://localhost:9999*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**clientesIdTransacoesPost**](TransacoesApi.md#clientesIdTransacoesPost) | **POST** /clientes/{id}/transacoes | Criar nova transação para um cliente |


<a id="clientesIdTransacoesPost"></a>
# **clientesIdTransacoesPost**
> SaldoLimite clientesIdTransacoesPost(id, transacaoRequest)

Criar nova transação para um cliente

Cria uma nova transação para o cliente especificado.  As transações podem ser de crédito (tipo \&quot;c\&quot;) ou débito (tipo \&quot;d\&quot;). Transações de débito são limitadas pelo saldo + limite do cliente. 

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.TransacoesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:9999");

    TransacoesApi apiInstance = new TransacoesApi(defaultClient);
    Integer id = 1; // Integer | ID do cliente
    TransacaoRequest transacaoRequest = new TransacaoRequest(); // TransacaoRequest | 
    try {
      SaldoLimite result = apiInstance.clientesIdTransacoesPost(id, transacaoRequest);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling TransacoesApi#clientesIdTransacoesPost");
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
| **transacaoRequest** | [**TransacaoRequest**](TransacaoRequest.md)|  | |

### Return type

[**SaldoLimite**](SaldoLimite.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Transação realizada com sucesso |  -  |
| **404** | Cliente não encontrado |  -  |
| **422** | Transação inválida. Possíveis motivos: * Saldo insuficiente para débito * Campos fora das especificações (tipo incorreto, descrição muito longa, etc.)  |  -  |

