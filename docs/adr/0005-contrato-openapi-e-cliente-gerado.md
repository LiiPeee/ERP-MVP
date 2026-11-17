# 0005. Contrato OpenAPI e cliente HTTP gerado

Data: 16/11/2026. Estado: aceita.

## Decisão
`openapi/openapi.json` é o contrato, gerado pelo backend (springdoc) e conferido pelo teste `OpenApiContractIT`, que falha se a API divergir do arquivo.
O cliente Angular (`frontend-angular/src/app/api`) é gerado com OpenAPI Generator (perfil Maven `generate-client`), versionado, e o CI confere que está igual ao gerado.

## Consequências
O contrato nunca diverge entre backend e frontend. Cada mudança de API exige atualizar o contrato e regenerar o cliente (comandos no README).
