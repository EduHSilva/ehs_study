# StudyFlow Design System

Os tokens editáveis vivem em `src/styles/global.css`, no bloco `:root` (modo claro) e `[data-theme=dark]` (modo escuro).

| Grupo | Tokens |
| --- | --- |
| Cor | `--color-*` com propósito semântico: `bg`, `surface`, `text`, `muted`, `primary`, `success`, `danger`, `warning` |
| Espaçamento | `--space-1` a `--space-6` |
| Raios | `--radius-sm` a `--radius-xl` |
| Tipografia | `--font-body`, `--font-heading` |

Para criar componentes e páginas, use tokens semânticos — por exemplo, `background: var(--color-surface)` — sem incluir hexadecimais diretamente no componente. O tema é salvo em `localStorage` sob a chave `studyflow-theme` e pode ser trocado pelo `ThemeToggle`.
