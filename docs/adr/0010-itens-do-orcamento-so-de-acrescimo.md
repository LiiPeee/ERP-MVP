# 0010. Itens do orçamento só de acréscimo

Data: 17/11/2026. Estado: aceita.

## Contexto
Os itens de um orçamento mudam enquanto ele é rascunho. O papel da aplicação não tem `DELETE` em lugar nenhum (regra do projeto),
e histórico de revisões é útil para auditoria.

## Decisão
- `quote_item` só recebe `INSERT`. Revisar os itens grava um **conjunto novo** (`revision + 1`) e `quote.items_revision` passa a
  apontar para ele. O conjunto antigo fica como histórico.
- O total **não é guardado**: o domínio o calcula dos itens (`Money` e `Quantity`, arredondamento `HALF_UP` por linha).
- Orçamento enviado não aceita revisão: cancela e refaz.

## Consequências
- O banco cresce com o histórico de revisões; a retenção desses conjuntos antigos entra na política de retenção.
- Leitura filtra pela revisão atual; qualquer relatório futuro deve fazer o mesmo.
