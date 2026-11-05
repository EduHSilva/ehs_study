# AGENTS.md

Monorepo do StudyFlow (painel de estudos). Dois clientes do mesmo backend:

- `web/` — SPA Vue 3 + TypeScript + Vite (pacote `studyflow-dashboard`), considerada estável.
- `app/` — app Android nativo em Kotlin + Jetpack Compose (Material 3).

O backend não está neste repositório; o contrato está em `web/todo/TODO_BACK.md`.

## Web (`web/`) — comandos

Execute a partir de `web/`:

- Dev: `npm run dev`
- Verificação: `npm run build` — é o check: roda `vue-tsc -b` (typecheck) e `vite build`.
- E2E: `npm run test:e2e` (Playwright + API mockada; primeira execução precisa de
  `npx playwright install chromium`). UI: `npm run test:e2e:ui`.
- Sem lint/format. Não invente scripts além dos existentes; typecheck isolado =
  `node node_modules/vue-tsc/bin/vue-tsc.js -b`.
- `vue-tsc` é chamado por caminho do binário (Windows-friendly), não `npx vue-tsc`.
- Docker (a partir da raiz): `docker compose up -d --build`. O front é servido pelo nginx dos
  microservices em `/app/study/` (container `study-ui`, rede `ehs_microservices_default`). Não
  publique porta no host; em produção use `EHS_MICROSERVICES_NETWORK=ehs_microservices_backend`.

## Web — arquitetura

As regras de `web/ARCHITECTURE.md` são seguidas; ignore os nomes de feature de exemplo
(workouts/diet). O código fica em `web/src/app/`:

- `pages/` — uma tela por arquivo; template + emits, sem lógica de domínio ou HTTP.
- `features/<nome>/` — `types.ts`, `<feature>.api.ts` (todo HTTP aqui, sem importar Vue) e
  `use<Feature>.ts` (estado, loading, validação).
- `composables/` — infra compartilhada: `httpClient`, `useNavigation`, `useTheme`, `useI18n`.
- `components/` — visuais compartilhados (`AppSidebar`, `ThemeToggle`, `LanguageToggle`).

Gotchas:

- Sem router: navegação pelo union `AppPage` + `currentPage` em `composables/useNavigation.ts`.
  Nova tela exige: estender `AppPage`, adicionar branch em `src/App.vue` e ligar `@navigate` na
  sidebar.
- Tudo só renderiza autenticado: `src/App.vue` atrás de `isAuthenticated`
  (`features/auth/useAuth.ts`); sem sessão aparece a `LandingPage`. Login sempre chama a API real
  (`POST /users/auth/login`); token em `localStorage` na chave `studyflow-auth`, enviado como
  Bearer pelo `httpClient`.
- `VITE_API_BASE_URL` ausente ⇒ `httpClient` lança em toda chamada (inclusive login). Dev precisa
  de `.env` + backend implementando `todo/TODO_BACK.md`.
- Endpoints de estudo vão pelo gateway em `/study/...` (`/study/notes/folders`, `/study/dashboard`,
  `/study/projects`, `/study/focus`, `/study/review/report`); auth (`/users/...`) e tasks
  (`/tasks/...`) ficam na raiz.
- E2E em `web/tests/e2e/` mockam a API com `page.route` (`support/mockApi.ts`); `.env.test` aponta
  `VITE_API_BASE_URL` para a origem do dev server (same-origin). Não precisam de backend.
- Perfil da sidebar vem de `useStudentProfile()` (usuário autenticado), nunca hardcoded por página.
- `localStorage` só em composables. Chaves: `studyflow-theme`, `studyflow-locale`, `studyflow-auth`.

## Android (`app/`) — comandos

Execute a partir de `app/` (projeto Gradle com o módulo `:app`, ou seja, `app/app/src`):

- Build/verificação: `./gradlew :app:assembleDebug` (Windows: `gradlew.bat :app:assembleDebug`).
- Testes JVM: `./gradlew :app:testDebugUnitTest`.
- Requer JDK 17+ (validado com o JBR do Android Studio) e Android SDK 36. `local.properties`
  (não versionado) define `sdk.dir`, `studyflow.apiBaseUrl` e `studyflow.googleWebClientId`;
  variáveis `ANDROID_HOME`, `STUDYFLOW_API_BASE_URL` e `STUDYFLOW_GOOGLE_WEB_CLIENT_ID` funcionam como
  fallback.
- Sem emulador conectado, a verificação é build + testes JVM.

## Android — arquitetura

- MVVM: cada feature em `feature/<nome>/` tem `ViewModel` com `StateFlow`; telas Compose só
  observam estado e emitem ações. Sem lógica de domínio nas telas.
- `data/remote` (DTOs + Retrofit), `data/repository` (HTTP), `data/mappers` (DTO → domínio
  tolerante), `domain/` (regras puras, testáveis na JVM), `di/AppContainer` (injeção manual).
- `core/` guarda infra: `network` (OkHttp/Retrofit/envelope), `session`, `settings`, `prefs`,
  `i18n` e `ui` (tema + componentes).
- Navegação com Navigation Compose (drawer + bottom bar). A tela de revisão existe, mas o item de
  menu fica desabilitado com **EM BREVE**, igual ao web.
- Sessão: `SessionManager` persiste token + usuário em `SharedPreferences` nas mesmas chaves do web
  (`studyflow-auth`, `studyflow-theme`, `studyflow-locale`). Resposta `401` expira a sessão; chamadas
  de `/tasks` enviam `X-User-ID`.
- HTTP em texto puro só no build `debug` (manifest overlay). Produção deve usar HTTPS.
- Regras de calendário/formatação/validação ficam em `domain/` e têm testes em
  `app/src/test/java/...` (`StudyCalendarTest`, `TaskFormatTest`, `AuthValidationTest`).

## Convenções (ambos)

- Web: TypeScript estrito com `verbatimModuleSyntax` — imports de tipo DEVEM usar
  `import type { ... }` ou o build falha.
- Strings de UI em pt-BR; o web alterna pt-BR/en com `useI18n` e o Android com `core/i18n/Strings`
  (mesmas chaves nos dois).
- Estilo: somente tokens semânticos, nunca hex cru nos componentes. Web usa `var(--color-*)` de
  `web/src/styles/global.css`; Android usa `MaterialTheme.colorScheme` e `StudyFlowTheme.colors`.
- Não versione `web/.env`, `app/local.properties`, `node_modules`, `dist` ou artefatos de build.
