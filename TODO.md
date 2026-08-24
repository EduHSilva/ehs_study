# TODO — Backend necessário para o StudyFlow

Todas as rotas são relativas a `VITE_API_BASE_URL` (ver `.env.example`). Exceto o login, toda rota deve aceitar `Authorization: Bearer <token>` e liberar CORS para a origem do frontend. Qualquer resposta não-2xx vira estado de erro com "Tentar novamente" na tela (o corpo do erro é ignorado).

## Auth

- [x] `POST /auth/login` — body `{ email, password }` → `200 { token: string, user: { name, email, course?, avatarUrl? } }`
  - O token é devolvido em todas as chamadas seguintes como Bearer.
  - `course` e `avatarUrl` alimentam a sidebar; sem eles caem em fallback ("—" / avatar padrão).
  - Sem `VITE_API_BASE_URL` configurada o login nem tenta chamada (httpClient lança erro).
- [ ] Validar o Bearer token nas demais rotas e responder `401` quando inválido (o front hoje trata como erro genérico; depois dá para tratar 401 → logout automático).

## Dashboard — painel inicial

- [ ] `GET /dashboard` →

    ```json
    {
      "weeklyProgress": { "completed": 14, "total": 20 },
      "focus": { "remainingSeconds": 1125, "subject": "Escrita do Capítulo 1", "running": true },
      "reminders": [ /* até 3, ver modelo Reminder abaixo */ ],
      "notes": [ { "id": "...", "week": "Semana 3", "title": "...", "subject": "..." } ]
    }
    ```

  - O toggle de lembrete do painel reutiliza `PATCH /reminders/:id`.

## Lembretes

Modelo `Reminder`: `{ id, title, schedule, subject, priority: 'Alta' | 'Média' | 'Baixa', completed }`

- [ ] `GET /reminders` → `Reminder[]`
- [ ] `POST /reminders` — body `{ title, schedule, subject, priority? }` → `Reminder` (`priority` default `'Média'`, enviado pelo front)
- [ ] `PATCH /reminders/:id` — body `{ completed: boolean }` → `Reminder`

`schedule` é string pronta para exibição (ex.: `"Hoje, 14:00"`, `"Sexta, 15:00"`).

## Notas

Modelos: `NoteFolder { id, name }`; `Note { id, folderId, title, content }`

- [ ] `GET /notes` → `{ folders: NoteFolder[], notes: Note[] }` (conteúdo completo das notas)
- [ ] `PUT /notes/:id` — body `{ title, content }` → `Note`
- [ ] `POST /notes/folders` — body `{ name }` → `NoteFolder`

## Projetos

Modelos: `Project { id, title, progress (0–100), status: 'Em andamento' | 'Concluído', advisor }`;
`ProjectStage { id, name, done }`; `ProjectMilestone { id, title, dueDate }`

- [ ] `GET /projects` → `Project[]`
- [ ] `GET /projects/:id` → `{ project: Project, stages: ProjectStage[], milestones: ProjectMilestone[] }`
- [ ] `PATCH /projects/:projectId/stages/:stageId` — body `{ done: boolean }` → `ProjectStage`
  - O front recalcula o progresso localmente ao alternar etapas.

`dueDate` é string exibível (ex.: `"12 Nov"`).

## Pomodoro

Modelo `FocusSession`: `{ id, title, minutes, completedAt }`

- [ ] `GET /pomodoro/sessions` → sessões concluídas **de hoje**: `FocusSession[]`
- [ ] *(opcional)* `POST /pomodoro/sessions` — body `{ title, minutes }` para registrar sessões finalizadas no timer

O cronômetro em si roda 100% no cliente; `completedAt` é string exibível (ex.: `"Concluído às 09:00"`).

## Revisão ABNT

- [ ] `GET /review/report` → último relatório do usuário:

    ```json
    {
      "documentTitle": "APOLOGIA DA HISTÓRIA E SEU REVERSO",
      "excerpt": "trecho exibido na pré-visualização",
      "flaggedExcerpt": "trecho marcado como similar",
      "similarityPercent": 14,
      "abntChecks": [ { "label": "Margens padrão (Esquerda 3cm, Direita 2cm)", "passed": true } ],
      "references": [ { "citation": "BLOCH, Marc. Apologia da história…", "status": "Válida" | "Atenção" } ]
    }
    ```

  - `status: 'Válida'` renderiza em verde; `'Atenção'` ainda usa o mesmo estilo (ajustar CSS depois, se quiser diferenciação).

## Observações gerais

- Datas/horários exibidos ao usuário estão chegando como strings formatadas pt-BR; se preferir ISO 8601, avise que migramos a formatação para o front.
- Não há refresh token; sessão vive no `localStorage` sob a chave `studyflow-auth`.
