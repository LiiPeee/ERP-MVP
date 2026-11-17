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
| 1 | Abra `/register`. Preencha empresa "Padaria Fictícia", nome "Ana Fictícia", e-mail `ana@exemplo.test`, senha de 12 ou mais caracteres. Clique em **Cadastrar**. | Vai para a **fila de orçamentos** (vazia). Em `/me`, aparecem seu nome, e-mail, empresa e papel `OWNER`. |
| 2 | Recarregue a página (F5). | Continua na fila: o cookie de refresh recupera a sessão. |
| 3 | Clique em **Sair**. Depois abra `/me` direto. | Volta para **Entrar**; não há acesso sem sessão. |
| 4 | Entre com a senha errada. | "E-mail ou senha incorretos." |
| 5 | Entre com um e-mail que não existe. | **A mesma** mensagem do passo 4. |
| 6 | Erre a senha 5 vezes seguidas. | Passa a ser bloqueado por 1 minuto, com a mesma mensagem genérica. |
| 7 | Cadastre uma segunda empresa (outro e-mail), em janela anônima. | A fila dela está vazia: não enxerga nada da primeira oficina. |
| 8 | Com `docker compose logs app`, procure o e-mail usado. | Nenhuma ocorrência de e-mail, CPF ou token nos logs. |
| 9 | No navegador, abra as ferramentas, aba Aplicativo, cookies. | `refresh` com HttpOnly, Secure e SameSite=Strict; nenhum token em `localStorage`. |

## Orçamento, do rascunho à aprovação (Fase 1, mais 5 minutos)

Faça pelo celular, ou com a janela estreita (390 px de largura). Use telefone e placa sintéticos.

| # | Ação | Resultado esperado |
|---|---|---|
| 10 | Na fila, toque em **Novo orçamento**. Em **Cliente novo**, nome "Cliente Sintético" e telefone `(11) 91234-5678`; **Cadastrar cliente**. | Abre o passo **2. Veículo**. O telefone aparece mascarado (`(11) *****-5678`). |
| 11 | Cadastre o veículo: placa `ABC1D23`, Fiat, Uno, 2014. | Abre o passo **3. Itens**. A placa aparece mascarada (`****D23`). |
| 12 | Item 1: "Troca de óleo", mão de obra, quantidade 1, preço 80. **Adicionar item**: "Filtro de óleo", peça, quantidade 2, preço `25,50`. **Salvar rascunho**. | Abre o orçamento como **rascunho**, total **R$ 131,00** calculado pelo servidor. |
| 13 | Cadastre outro cliente com o mesmo telefone. | "Já existe um cliente com este telefone." |
| 14 | No rascunho, **Confirmar e gerar link** e confirme no diálogo. | Estado **enviado**, validade de 7 dias e o **link** para copiar. Ele só aparece agora. |
| 15 | Copie o link e abra em **janela anônima** (sem login). | Página do cliente: oficina, veículo (placa só no final), itens, total, validade, **Aprovar orçamento** e **Recusar**. Sem menu. |
| 16 | Recarregue a página do cliente. | Nada mudou: abrir o link não decide. |
| 17 | Toque em **Aprovar orçamento**. Recarregue. | "Orçamento aprovado." continua aparecendo e já não há botões. |
| 18 | Na fila do dono, olhe **Fechados**. | O orçamento aparece como **aprovado**. |
| 19 | Crie outro orçamento, envie, **Cancelar orçamento** e abra o link antigo. | "Este link não está mais disponível", a mesma mensagem de um link inventado. |
| 20 | Com `docker compose logs app`, procure o telefone, a placa e o token do link. | Nenhuma ocorrência; o caminho do link sai como `/o/{token}`. |

## Encerrar

```bash
docker compose down --volumes
```
