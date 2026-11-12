# Arquitetura do frontend

O projeto segue uma organização por domínio (feature-first), apropriada para Nuxt/Vue:

```
app/
  pages/                    # rotas e composição da tela; sem regras de domínio ou HTTP
  components/               # componentes visuais compartilhados
  composables/              # infraestrutura compartilhada (ex.: cliente autenticado)
  features/
    workouts/
      types.ts              # modelos do domínio
      workout.api.ts        # contratos e chamadas da API
      useWorkouts.ts        # estado, validação e casos de uso da tela
    diet/
    habits/
    students/
  types/                    # tipos transversais
```

Regras de manutenção:

- Uma página importa um `use<Feature>` e concentra-se no template e nos metadados da rota.
- Chamadas HTTP ficam em `<feature>.api.ts`; esses módulos não dependem de Vue.
- Regras de formulário, transformação de payload e estados de carregamento ficam no composable da feature.
- Acesso a `localStorage` fica em `*.repository.ts`, nunca em páginas.
- Tipos são declarados no domínio que os possui e só vão para `app/types` quando forem realmente transversais.

## Shell e carregamento (SPA)

- `src/App.vue` é o shell autenticado: mantém sidebar, widget do Pomodoro e troca só a página
  (`<component :is>`). As páginas renderizam apenas `<section class="content">`; não montam
  sidebar nem `<main class="app-shell">`.
- Primeiro carregamento usa `components/PageSkeleton.vue` (com `layout` por tipo de tela), nunca
  um texto centralizado de "Carregando…". O cabeçalho da página continua visível.
- O estado das features fica em refs de módulo com o par `loaded`/`inFlight`: ao revisitar uma
  tela o conteúdo anterior aparece na hora e `load()` revalida em silêncio; o skeleton só
  aparece antes do primeiro sucesso. Operações em andamento são coalescidas por `inFlight`.
- `composables/useSessionCache.ts` limpa esses caches no logout/401 (`notifyUnauthorized`) e no
  login, para não exibir dados do usuário anterior.
- O hover/focus no item do menu dispara `load()` da página (prefetch sob intenção); ao clicar,
  o conteúdo tende a já estar pronto.

