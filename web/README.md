# StudyFlow — Web

SPA em Vue 3 + TypeScript + Vite do StudyFlow (pacote `studyflow-dashboard`). É o cliente web do
mesmo backend usado pelo app Android em `../app`.

## Requisitos

- Node.js 20+
- Backend implementando o contrato de `todo/TODO_BACK.md`

## Configuração

Copie `.env.example` para `.env` e preencha:

```bash
VITE_API_BASE_URL=http://localhost:3000
VITE_GOOGLE_CLIENT_ID=          # opcional: habilita "Entrar com Google"
```

Sem `VITE_API_BASE_URL` todas as chamadas falham (inclusive o login). Não há modo mock no runtime.
Use `VITE_API_BASE_URL=/` para consumir a API na mesma origem (necessário no Docker, atrás do
nginx dos microservices); a barra final é normalizada pelo `httpClient`.

No build de produção o app é servido em `/app/study/`. O Docker define
`VITE_BASE_PATH=/app/study/`; em dev/E2E o padrão continua `/`.

## Comandos

Execute a partir desta pasta (`web/`):

- `npm install` — instala dependências.
- `npm run dev` — servidor de desenvolvimento.
- `npm run build` — **verificação do projeto**: roda `vue-tsc -b` (typecheck) e depois `vite build`.
- `npm run preview` — serve o build.
- `npm run test:e2e` — Playwright com API mockada por `page.route` (primeira execução pede
  `npx playwright install chromium`).
- `npm run test:e2e:ui` — Playwright em modo UI.
- `npm run test:e2e:real` — Playwright contra backend real (`E2E_API_BASE_URL`).

Não há lint/format configurado; não crie scripts além dos existentes.

## Estrutura

```
src/app/
├── pages/          # uma tela por arquivo (template + emits)
├── features/       # types, <feature>.api.ts (HTTP) e use<Feature>.ts (estado)
├── composables/    # httpClient, useNavigation, useTheme, useI18n
└── components/     # AppSidebar, ThemeToggle, LanguageToggle
```

Detalhes de camadas em `ARCHITECTURE.md` e tokens visuais em `DESIGN_SYSTEM.md`.

## Observações

- Não há router: a navegação usa o union `AppPage` + `currentPage` em
  `composables/useNavigation.ts`; para adicionar uma tela, estenda o union, adicione o branch em
  `src/App.vue` e ligue o `@navigate` na sidebar.
- O app inteiro fica atrás de `isAuthenticated` (`features/auth/useAuth.ts`); sem sessão aparece a
  `LandingPage`.
- Sessão/tema/idioma usam `localStorage` com as chaves `studyflow-auth`, `studyflow-theme` e
  `studyflow-locale` — as mesmas do app Android.
- Endpoints de estudo passam pelo gateway em `/study/...`; auth (`/users/...`) e tasks
  (`/tasks/...`) ficam na raiz.
