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
