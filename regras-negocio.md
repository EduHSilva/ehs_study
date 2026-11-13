# Regras de negócio — EHS Estudos (StudyFlow)

> Plataforma (auth, roles, isolamento): `D:/Projetos/ehs_microservices/regras-negocio.md`.
> O produto usa dois serviços: `study` (projetos, notas, foco) e `tasks` (tarefas).

## Acesso

- Login com `app: STUDY` (role `STUDY`). Recursos adicionais já vinculados ao usuário
  usam o mesmo escopo.

## Tarefas

- Tipos: `UNIQUE` (data final), `DAILY` (todo dia), `WEEKLY` (dias fixos da semana),
  `MONTHLY` (dia fixo do mês) e `WEEKLY_GOAL` (meta de conclusões por semana).
- Status: `PENDING`, `WORKING`, `DONE`, `DELAYED`, `DELETED`.
- Toda tarefa nasce `PENDING`; `DELAYED` **não** é escolhido pelo usuário:
  - `UNIQUE` com data final vencida e status `PENDING` vira `DELAYED`;
  - `WORKING` e `DONE` nunca são sobrescritos;
  - tarefas recorrentes (`DAILY`, `WEEKLY`, `MONTHLY`, `WEEKLY_GOAL`) nunca atrasam.
  - Estender o prazo faz a tarefa voltar para `PENDING`.
- `UNIQUE` concluída guarda a data de conclusão; reabrir limpa essa data.
- Tarefas recorrentes não têm "concluída para sempre": cada dia concluído é um registro
  de conclusão (um por dia). Marcar `DONE` cria o registro do dia, `PENDING` remove,
  `WORKING` só limpa o dia; `DELAYED` é inválido.
- `WEEKLY` usa dias ISO (1 = segunda … 7 = domingo) e só pode ser concluída nos dias
  configurados.
- `MONTHLY` usa o dia do mês (1..31); em meses mais curtos vale o último dia
  (ex.: dia 31 em fevereiro ⇒ 28/29).
- `WEEKLY_GOAL` pode ser concluída em qualquer dia; a semana começa na segunda. Enquanto a
  meta não é atingida, a tarefa aparece pendente em todos os dias da semana; ao atingir,
  as pendências dos demais dias somem e só ficam visíveis os dias concluídos. Na semana
  seguinte o progresso zera.
- A data de conclusão não pode estar no futuro nem fora do intervalo início/fim.
- Data de início padrão = hoje. A listagem padrão cobre hoje até +7 dias.
- A tarefa pode referenciar projeto/etapa (opcional, por UUID entre serviços).
- Exclusão é soft delete (`DELETED`).

## Estatísticas de tarefas recorrentes

- `DAILY`, `WEEKLY` e `MONTHLY`: conta apenas os dias agendados; falhas são os dias
  passados sem conclusão (o dia de hoje ainda não conta como falha). A adesão é
  `concluídos até hoje / dias agendados até hoje`.
- `WEEKLY_GOAL`: avalia semanas fechadas (meta atingida = semana ok, senão falha) e mostra
  o progresso da semana corrente separadamente.
- O painel considera as últimas 12 semanas e respeita o filtro por projeto.


## Projetos, etapas e versões

- Projeto tem título, status, orientador, etapas e versões.
- Etapa tem nome e "concluída"; o progresso do projeto é o percentual de etapas
  concluídas (inteiro; 0 quando não há etapas).
- Versão tem título e prazo; no app Android o prazo é sinalizado como `OVERDUE` quando já
  passou e `SOON` até 3 dias.
- Ordenação de tarefas por status (aguardando → em andamento → concluída) e depois nome.

## Notas e foco

- Pastas contêm notas (excluir a pasta remove as notas).
- Sessões de foco registram título, minutos e data de conclusão.
- O Pomodoro tem foco, intervalo e descanso longo configuráveis (minutos e ciclos). As
  preferências são salvas por aparelho (localStorage no web, SharedPreferences no Android)
  e valem a partir do próximo ciclo.
- O dashboard considera a semana de segunda a domingo e devolve até 10 tarefas e as 5
  notas mais recentes.

## Não implementado (não usar como regra)

- Notas/nota por disciplina, boletim ou cálculo de média: não existe entidade de
  disciplina nem nota no backend.
- Revisão de texto/ABNT: o endpoint `GET /study/review/report` não existe no serviço. O
  item foi removido da navegação do web e do Android.
