# StudyFlow

Painel de estudos para rotina acadêmica: cronograma, tarefas, notas, pomodoro e acompanhamento de
projetos/TCC. O repositório contém dois clientes do mesmo backend:

| Pasta | Stack | Descrição |
| --- | --- | --- |
| `web/` | Vue 3 + TypeScript + Vite | SPA do painel, com testes E2E em Playwright |
| `app/` | Kotlin + Jetpack Compose (Android) | App nativo Android com as mesmas features |

## Começando

### Web

```bash
cd web
npm install
cp .env.example .env   # configure VITE_API_BASE_URL
npm run dev
```

Verificação: `npm run build` (typecheck + bundle). E2E: `npm run test:e2e`.
Detalhes em [`web/README.md`](web/README.md).

### Android

```bash
cd app
./gradlew :app:assembleDebug        # Windows: gradlew.bat :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Configure `app/local.properties` com `sdk.dir`, `studyflow.apiBaseUrl` e, opcionalmente,
`studyflow.googleWebClientId`. Detalhes em [`app/README.md`](app/README.md).

## Backend

Não há backend neste repositório. O contrato esperado está em
[`web/todo/TODO_BACK.md`](web/todo/TODO_BACK.md): auth em `/users/...`, tarefas em `/tasks/...` e
os demais recursos pelo gateway em `/study/...`.

## Docker / Deploy

O front web roda em um container nginx próprio e é publicado pelo nginx dos
microservices em **`/app/study/`**, sem publicar porta no host:

```bash
docker compose up -d --build
# http://localhost/app/study/
```

- A rede externa padrão é `ehs_microservices_default`. Em produção use
  `EHS_MICROSERVICES_NETWORK=ehs_microservices_backend`.
- `VITE_BASE_PATH=/app/study/` e `VITE_API_BASE_URL=/` (same-origin) são definidos no build.
- A imagem publicada pelo GitHub Actions é `ghcr.io/eduhsilva/study-ui`
  (tags `latest` e `<sha>`), gerada por `.github/workflows/build.yml`. Para usar a imagem
  publicada em vez de buildar localmente:
  `STUDY_UI_IMAGE=ghcr.io/eduhsilva/study-ui:latest docker compose pull study-ui && docker compose up -d --no-build`.
- A rota `/app/study/` fica em `infra/nginx/conf.d/gateway.conf` no repositório dos
  microservices e aponta para o container `study-ui` na porta 80.

## Documentação

- [`web/ARCHITECTURE.md`](web/ARCHITECTURE.md) — camadas e organização do front web.
- [`web/DESIGN_SYSTEM.md`](web/DESIGN_SYSTEM.md) — tokens e padrões visuais.
- [`AGENTS.md`](AGENTS.md) — convenções para agentes/contribuidores nos dois clientes.
