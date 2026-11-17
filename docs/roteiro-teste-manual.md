# Roteiro de teste manual (5 minutos)

Use só dados sintéticos. Requisitos: Docker e o `.env` criado a partir do `.env.example`.

## Preparação (1 min)

```bash
cp .env.example .env
docker compose up --build --detach --wait
```

Abra <http://localhost:8080>.

## Passos

| # | Ação | Resultado esperado |
|---|---|---|
| 1 | Abra `/register`. Preencha empresa "Padaria Fictícia", nome "Ana Fictícia", e-mail `ana@exemplo.test`, senha de 12 ou mais caracteres. Clique em **Cadastrar**. | Vai para **Quem sou eu** com seu nome, e-mail, empresa e papel `OWNER`. |
| 2 | Recarregue a página (F5). | Continua em **Quem sou eu**: o cookie de refresh recupera a sessão. |
| 3 | Clique em **Sair**. Depois abra `/me` direto. | Volta para **Entrar**; não há acesso sem sessão. |
| 4 | Entre com a senha errada. | "E-mail ou senha incorretos." |
| 5 | Entre com um e-mail que não existe. | **A mesma** mensagem do passo 4. |
| 6 | Erre a senha 5 vezes seguidas. | Passa a ser bloqueado por 1 minuto, com a mesma mensagem genérica. |
| 7 | Cadastre uma segunda empresa (outro e-mail), em janela anônima. | Só enxerga a própria empresa em **Quem sou eu**. |
| 8 | Com `docker compose logs app`, procure o e-mail usado. | Nenhuma ocorrência de e-mail, CPF ou token nos logs. |
| 9 | No navegador, abra as ferramentas, aba Aplicativo, cookies. | `refresh` com HttpOnly, Secure e SameSite=Strict; nenhum token em `localStorage`. |

## Encerrar

```bash
docker compose down --volumes
```
