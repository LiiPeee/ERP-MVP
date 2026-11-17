# 0008. Criptografia de campo e índice cego

Data: 17/11/2026. Estado: aceita.

## Contexto
Telefone, e-mail e placa de clientes da oficina são dado pessoal de terceiro. O banco não pode guardá-los em claro, mas a
oficina precisa achar um cliente pelo telefone e o sistema precisa impedir duplicidade.

## Decisão
- Porta `FieldProtector` (`encrypt`, `decrypt`, `blindIndex`), com adaptador sobre o **Tink** (AES-256-GCM e HMAC-SHA256).
- A cifra é presa à empresa e ao campo (dados associados). O índice cego também, com **chave separada**.
- Chaves de cifrar rotacionáveis (várias ativas, uma assina); chave do índice sem rotação.
- A busca por nome não usa criptografia: o nome fica em claro e há uma coluna `name_search` normalizada (sem acento e sem
  maiúsculas). Decisão consciente: nome é pouco sensível frente ao custo de cifrar e perder a busca.
- Alternativas descartadas: JCA puro (formato, IV e rotação por nossa conta) e `spring-security-crypto` (sem identificação de
  chave nem rotação).

## Consequências
- Nova dependência (`tink`), justificada em `docs/dependencias.md`; traz protobuf e gson.
- Perder as chaves perde o dado. O índice cego é permanente.
- Falta uma rotina de regravação para aposentar chaves antigas.
