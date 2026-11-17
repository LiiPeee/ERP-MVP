# 0003. Senhas, sessão e tokens

Data: 16/11/2026. Estado: aceita.

## Decisão
- **Senhas:** Argon2id (19 MiB, t=2, p=1) com BouncyCastle; bloqueio progressivo (5 falhas, 1 min dobrando até 1 h); mensagens idênticas para usuário inexistente e senha errada, com equalização de tempo.
- **Acesso:** JWT HS256 de 15 minutos, com `kid` e rotação de várias chaves vindas do ambiente (Base64, mínimo de 32 bytes). A aplicação não sobe sem chave.
- **Refresh:** token aleatório de 256 bits, só o hash SHA-256 no banco, com rotação a cada uso. O reuso de um token já trocado revoga a família inteira. Validade de 7 dias, teto de 30 dias de sessão, `SELECT FOR UPDATE` contra corrida.
- **Navegador:** refresh em cookie `HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/api/auth`; token de acesso só em memória; CSRF por cookie `XSRF-TOKEN`.

## Consequências
- Roubo do banco não entrega refresh tokens utilizáveis.
- Trocar a chave de assinatura é operação sem indisponibilidade (nova chave ativa, antiga ainda aceita até expirar).
- Limite em memória por instância: vale para uma única aplicação; mudar isso exige nova decisão (ver README, decisões em aberto).
