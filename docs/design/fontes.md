# Fontes

Hospedadas localmente em `frontend-angular/public/fonts/` (servidas pela própria aplicação). Nenhuma fonte vem de CDN ou de serviço externo; a CSP só permite a própria origem.

| Família | Uso | Pesos | Licença | Origem |
|---|---|---|---|---|
| Schibsted Grotesk | texto, rótulos e títulos | 500, 600, 700 | SIL Open Font License 1.1 | pacote npm `@fontsource/schibsted-grotesk` 5.3.0 |
| IBM Plex Mono | valores em reais e datas (numerais tabulares) | 400, 500 | SIL Open Font License 1.1 | pacote npm `@fontsource/ibm-plex-mono` 5.3.0 |

- Os arquivos são o subconjunto **latin** em `woff2`, o que cobre o português (acentos, cedilha, til) e o sinal de menos tipográfico (U+2212). Texto fora desse conjunto cairia na fonte do sistema.
- O texto integral das licenças acompanha os arquivos: `public/fonts/LICENSE-schibsted-grotesk.txt` e `public/fonts/LICENSE-ibm-plex-mono.txt`.
- Os pacotes `@fontsource/*` foram baixados numa pasta temporária, só para copiar os arquivos; **não** são dependência do projeto (`package.json` não os lista).
- A licença OFL permite uso, cópia e redistribuição junto com o software; não permite vender as fontes isoladamente. Não tratei de nome reservado (as fontes mantêm o nome original). Revisão jurídica da licença não foi feita.
- Regra de uso: números sempre em IBM Plex Mono (classe `.num`, `font-variant-numeric: tabular-nums`); todo o resto em Schibsted Grotesk. Texto corrido longo, se um dia houver, volta a ser decisão de design.

## Descartadas

Newsreader e IBM Plex Sans eram da direção A, que não foi escolhida; os arquivos foram removidos.
