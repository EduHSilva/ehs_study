# AGENTS.md

Vue 3 + TypeScript + Vite SPA ("StudyFlow", pkg `studyflow-dashboard`). Student study dashboard. No router lib, no backend in-repo, no tests.

## Commands

- Dev: `npm run dev`
- Verify: `npm run build` — this IS the check: runs `vue-tsc -b` (typecheck) then `vite build`.
- No lint, format, or test tooling is configured. Don't invent npm scripts; typecheck alone = `node node_modules/vue-tsc/bin/vue-tsc.js -b`.
- `vue-tsc` is invoked by bin path (Windows-friendly), not `npx vue-tsc`. Keep it that way.

## Architecture

Layering rules in ARCHITECTURE.md are followed; ignore its example feature names (workouts/diet/etc.) — the real feature is `src/app/features/dashboard`. Code lives under `src/app/`:

- `pages/` — one component per screen; template + emits only, no domain logic or HTTP.
- `features/<name>/` — `types.ts`, `<feature>.api.ts` (all HTTP here, must not import Vue), `use<Feature>.ts` (state, loading, validation).
- `composables/` — shared infra: `httpClient`, `useNavigation`, `useTheme`, `useI18n`.
- `components/` — shared visuals (`AppSidebar`, `ThemeToggle`, `LanguageToggle`).

Gotchas:

- Navigation has NO router. Pages are switched by the `AppPage` union + module-level `currentPage` ref in `composables/useNavigation.ts`. Adding a page requires: extend `AppPage`, add a `v-if`/`v-else-if` branch in `src/App.vue`, wire `@navigate` through the sidebar.
- All routes render only when authenticated: `src/App.vue` gates everything behind `isAuthenticated` (`features/auth/useAuth.ts`); unauthenticated users see `LandingPage`. Login ALWAYS hits the real API (`POST /auth/login`) — no mock fallback anywhere since mocks were removed; token lives in localStorage key `studyflow-auth` and is attached as Bearer by `httpClient`.
- `VITE_API_BASE_URL` unset ⇒ httpClient throws on every call (login included). Dev requires `.env` + backend implementing the contract in `TODO.md`.
- Sidebar profile comes from `useStudentProfile()` (auth user), never hardcoded per page.
- localStorage access stays out of pages (composables only). Keys in use: `studyflow-theme`, `studyflow-locale`, `studyflow-auth`.

## Conventions

- Strict TS with `verbatimModuleSyntax`: type-only imports MUST use `import type { ... }` or the build fails.
- UI strings are pt-BR; `useI18n` adds pt-BR/en switching (existing pages don't all use it yet).
- Styling: semantic tokens only, never raw hex in components — use `var(--color-surface)` etc. from the `:root` / `[data-theme=dark]` blocks in `src/styles/global.css` (see DESIGN_SYSTEM.md for token groups: `--color-*`, `--space-1..6`, `--radius-*`).
