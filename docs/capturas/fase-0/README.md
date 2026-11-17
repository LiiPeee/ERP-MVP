# Capturas da Fase 0

Geradas por `e2e/capturas/capturar.mjs` (desktop 1280x800 e celular 390x844): cadastro, quem sou eu, entrar e entrar com erro.

## Autocrítica visual

As telas da Fase 0 são **rascunhos descartáveis, sem identidade visual**, por decisão da fase: o design é a Fase 0.5.
Contra a lista do `CLAUDE.md`, nada genérico de IA aparece (sem painel escuro, gradientes, cartões, emoji), mas isso é só porque **não há estilo algum**.
Problemas conhecidos, a resolver na Fase 0.5:

- Tipografia padrão do navegador (serifada), sem hierarquia nem tokens de design.
- Rótulos e campos lado a lado, campos estreitos; no celular o e-mail longo é cortado no campo.
- Os links "Entrar" e "Cadastrar" aparecem grudados no topo.
- Sem foco visível customizado nem verificação de contraste (WCAG AA) feita; só o comportamento por teclado é testado no e2e.
- A mensagem de erro é texto simples, sem destaque nem ligação visual com o campo.
