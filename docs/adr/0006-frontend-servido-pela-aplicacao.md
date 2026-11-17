# 0006. Frontend servido pela própria aplicação

Data: 16/11/2026. Estado: aceita.

## Decisão
O Angular 22 (standalone, signals, zoneless, TypeScript estrito) é compilado no Docker e servido pelo Spring (`SPRING_WEB_RESOURCES_STATIC_LOCATIONS`). Mesma origem para telas e API, o que permite CSP com `'self'` apenas, CORS fechado e cookies `SameSite=Strict`.
Sem scripts ou estilos inline, sem recursos de terceiros, sem NgRx, sem tema pronto de biblioteca de UI.

## Consequências
Uma única implantação. Em desenvolvimento, o frontend roda pelo servidor do Angular e precisa de proxy para a API.
