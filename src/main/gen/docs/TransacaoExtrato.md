

# TransacaoExtrato


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**valor** | **Integer** | Valor da transação em centavos |  |
|**tipo** | [**TipoEnum**](#TipoEnum) | Tipo da transação (c para crédito, d para débito) |  |
|**descricao** | **String** | Descrição da transação |  |
|**realizadaEm** | **OffsetDateTime** | Data e hora da realização da transação |  |



## Enum: TipoEnum

| Name | Value |
|---- | -----|
| C | &quot;c&quot; |
| D | &quot;d&quot; |



