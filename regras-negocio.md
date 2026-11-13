# Regras de negócio — EHS Estudos (StudyFlow)

> Plataforma (auth, roles, isolamento): `D:/Projetos/ehs_microservices/regras-negocio.md`.
> O produto usa dois serviços: `study` (projetos, notas, foco) e `tasks` (tarefas).

## Acesso

- Login com `app: STUDY` (role `STUDY`). Recursos adicionais já vinculados ao usuário
  usam o mesmo escopo.

## Tarefas

- Tipos: `UNIQUE` (data final) e `DAILY` (repetição diária).
- Status: `PENDING`, `WORKING`, `DONE`, `DELAYED`, `DELETED`.
- Toda tarefa nasce `PENDING`; `DELAYED` **não** é escolhido pelo usuário:
  - `UNIQUE` com data final vencida e status `PENDING` vira `DELAYED`;
  - `WORKING` e `DONE` nunca são sobrescritos;
  - `DAILY` nunca atrasa.
  - Estender o prazo faz a tarefa voltar para `PENDING`.
- `UNIQUE` concluída guarda a data de conclusão; reabrir limpa essa data.
- `DAILY` não tem "concluída para sempre": cada dia concluído é um registro de conclusão
  (um por dia). Marcar `DONE` cria o registro do dia, `PENDING` remove, `WORKING` só
  limpa o dia; `DELAYED` é inválido.
- A data de conclusão não pode estar no futuro nem fora do intervalo início/fim.
- Data de início padrão = hoje. A listagem padrão cobre hoje até +7 dias.
- A tarefa pode referenciar projeto/etapa (opcional, por UUID entre serviços).
- Exclusão é soft delete (`DELETED`).

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
- O dashboard considera a semana de segunda a domingo e devolve até 10 tarefas e as 5
  notas mais recentes.

## Não implementado (não usar como regra)

- Notas/nota por disciplina, boletim ou cálculo de média: não existe entidade de
  disciplina nem nota no backend.
- Revisão de texto/ABNT: a tela chama `GET /study/review/report`, endpoint que não existe
  no serviço — a página só exibe erro.
