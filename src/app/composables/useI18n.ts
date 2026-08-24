import { computed, ref } from 'vue'

export type Locale = 'pt-BR' | 'en'
type Messages = Record<string, string>
const messages: Record<Locale, Messages> = {
  'pt-BR': {
    dashboard:'Dashboard', pomodoro:'Pomodoro', notes:'Anotações', reminders:'Lembretes', projects:'TCC & Trabalhos', review:'Revisão TCC',
    language:'EN', todayReminders:'Lembretes de Hoje', notesTitle:'Anotações de Aula', projectsTitle:'Projetos Acadêmicos', reviewTitle:'Revisão e Formatação ABNT', timerTitle:'Cronômetro Pomodoro', save:'Salvar', start:'Começar', reset:'Reiniciar', sessions:'Sessões de Hoje',
    landingTitle:'Seus estudos, em fluxo.', landingSubtitle:'Organize cronograma, notas, lembretes e sessões de foco em um único painel pensado para a rotina acadêmica.', landingPointTimer:'Cronômetro Pomodoro e progresso semanal', landingPointNotes:'Notas e fichamentos organizados por semana', landingPointReminders:'Lembretes de TCC, provas e entregas', loginTitle:'Acessar conta', loginSubtitle:'Entre para continuar para o seu painel de estudos.', emailLabel:'E-mail', passwordLabel:'Senha', emailPlaceholder:'voce@universidade.br', loginAction:'Entrar', loggingIn:'Entrando…',
    retry:'Tentar novamente', greeting:'Olá, {name}!', studentFallback:'Estudante', logoutAction:'Sair da conta',
    dashboardWelcome:'Seja bem-vindo de volta ao seu espaço de estudos. Veja o resumo de hoje.', weeklyProgressTitle:'Progresso da Semana', weeklyProgressBefore:'Você concluiu', of:'de', weeklyProgressAfter:'metas estabelecidas para esta semana.', weeklyProgressCheer:'Excelente ritmo! Continue assim.', focusTitle:'Foco Ativo', focusTag:'Sessão', focusSubject:'Foco:', seeAll:'Ver todos', recentNotes:'Anotações Recentes', subjectLabel:'Assunto:', dashboardLoading:'Sincronizando seu espaço de estudos…', dashboardError:'Não foi possível carregar o dashboard.',
    timerSubtitle:'Gerencie seu tempo de estudo de forma eficiente e mantenha o foco ativo.', focusMode:'Foco (25m)', breakMode:'Intervalo (5m)', sessionCounter:'Sessão {current} de {total}', pause:'Pausar', noSessions:'Nenhuma sessão concluída hoje.', pomodoroLoading:'Carregando sessões…',
    notesSubtitle:'Crie e estruture suas notas acadêmicas com formatação rica.', foldersTitle:'Pastas / Seções', noFolders:'Nenhuma pasta ainda.', selectNotePlaceholder:'Selecione uma anotação', contentPlaceholder:'Escreva sua anotação aqui…', savedEarlier:'Salvo há 2 minutos', savedNow:'Salvo agora', saving:'Salvando…', notesLoading:'Carregando anotações…',
    remindersSubtitle:'Organize suas tarefas e mantenha os prazos sob controle.', noReminders:'Nenhum lembrete por aqui ainda. Use o ＋ para criar o primeiro.', newReminderTitle:'Novo lembrete', todayLabel:'Hoje', generalSubject:'Geral', remindersLoading:'Carregando lembretes…',
    projectsSubtitle:'Acompanhe o desenvolvimento de suas pesquisas, monografias e TCC.', myProjects:'Meus Trabalhos', percentCompleted:'{progress}% Concluído', advisorLabel:'Orientador:', projectStatusInProgress:'Em andamento', projectStatusDone:'Concluído', stagesTitle:'Lista de Etapas', milestonesTitle:'Próximos Marcos', milestoneLabel:'MARCO {index}', dueDateLabel:'Prazo: {date}', noProjects:'Nenhum projeto ainda.', noMilestones:'Nenhum marco cadastrado.', projectsLoading:'Carregando projetos…',
    reviewSubtitle:'Utilize nossa validação inteligente para conferir plágio, regras ABNT e bibliografia.', previewA4:'Pré-visualização A4', tabPlagiarism:'Plágio', tabAbnt:'Formatação ABNT', tabReferences:'Referências', similarityTitle:'Índice de Similaridade', similarityText:'Trechos similares correspondem a {percent}% do documento.', referencesHeading:'Referências Analisadas', referenceValid:'Válida', referenceAttention:'Atenção', reviewLoading:'Analisando documento…',
    themeDarkLabel:'Escuro', themeLightLabel:'Claro', themeDarkAction:'Ativar modo escuro', themeLightAction:'Ativar modo claro',
    invalidEmail:'Informe um e-mail válido.', passwordTooShort:'A senha deve ter pelo menos 6 caracteres.', loginFailed:'Não foi possível entrar. Verifique suas credenciais.',
    dashboardLoadFailed:'Não foi possível sincronizar seus dados.', reminderSaveFailed:'Não foi possível salvar o lembrete.', remindersLoadFailed:'Não foi possível carregar os lembretes.', reminderCreateFailed:'Não foi possível criar o lembrete.',
    notesLoadFailed:'Não foi possível carregar suas anotações.', noteSaveFailed:'Não foi possível salvar a anotação.', folderCreateFailed:'Não foi possível criar a pasta.',
    projectsLoadFailed:'Não foi possível carregar seus projetos.', projectDetailsLoadFailed:'Não foi possível carregar os detalhes do projeto.', stageSaveFailed:'Não foi possível salvar a etapa.',
    pomodoroLoadFailed:'Não foi possível carregar suas sessões.', reviewLoadFailed:'Não foi possível carregar a análise de revisão.'
  },
  en: {
    dashboard:'Dashboard', pomodoro:'Pomodoro', notes:'Notes', reminders:'Reminders', projects:'Thesis & Projects', review:'Thesis review',
    language:'PT', todayReminders:"Today's reminders", notesTitle:'Class notes', projectsTitle:'Academic projects', reviewTitle:'ABNT review & formatting', timerTitle:'Pomodoro timer', save:'Save', start:'Start', reset:'Reset', sessions:"Today's sessions",
    landingTitle:'Your studies, in flow.', landingSubtitle:'Organize your schedule, notes, reminders and focus sessions in one dashboard built for academic life.', landingPointTimer:'Pomodoro timer and weekly progress', landingPointNotes:'Week-by-week notes and summaries', landingPointReminders:'Thesis, exam and deadline reminders', loginTitle:'Sign in', loginSubtitle:'Sign in to continue to your study dashboard.', emailLabel:'Email', passwordLabel:'Password', emailPlaceholder:'you@university.edu', loginAction:'Sign in', loggingIn:'Signing in…',
    retry:'Try again', greeting:'Hi, {name}!', studentFallback:'Student', logoutAction:'Sign out',
    dashboardWelcome:"Welcome back to your study space. Here is today's summary.", weeklyProgressTitle:'Weekly progress', weeklyProgressBefore:'You completed', of:'of', weeklyProgressAfter:'goals set for this week.', weeklyProgressCheer:'Great pace! Keep it up.', focusTitle:'Active focus', focusTag:'Session', focusSubject:'Focus:', seeAll:'See all', recentNotes:'Recent notes', subjectLabel:'Subject:', dashboardLoading:'Syncing your study space…', dashboardError:'Could not load the dashboard.',
    timerSubtitle:'Manage your study time efficiently and stay focused.', focusMode:'Focus (25m)', breakMode:'Break (5m)', sessionCounter:'Session {current} of {total}', pause:'Pause', noSessions:'No sessions completed today.', pomodoroLoading:'Loading sessions…',
    notesSubtitle:'Create and structure your academic notes with rich formatting.', foldersTitle:'Folders / Sections', noFolders:'No folders yet.', selectNotePlaceholder:'Select a note', contentPlaceholder:'Write your note here…', savedEarlier:'Saved 2 minutes ago', savedNow:'Saved now', saving:'Saving…', notesLoading:'Loading notes…',
    remindersSubtitle:'Organize your tasks and keep deadlines under control.', noReminders:'No reminders here yet. Use ＋ to create the first one.', newReminderTitle:'New reminder', todayLabel:'Today', generalSubject:'General', remindersLoading:'Loading reminders…',
    projectsSubtitle:'Track the progress of your research, papers and thesis.', myProjects:'My projects', percentCompleted:'{progress}% completed', advisorLabel:'Advisor:', projectStatusInProgress:'In progress', projectStatusDone:'Completed', stagesTitle:'Stage list', milestonesTitle:'Upcoming milestones', milestoneLabel:'MILESTONE {index}', dueDateLabel:'Due: {date}', noProjects:'No projects yet.', noMilestones:'No milestones yet.', projectsLoading:'Loading projects…',
    reviewSubtitle:'Use our smart validation to check plagiarism, ABNT rules and bibliography.', previewA4:'A4 preview', tabPlagiarism:'Plagiarism', tabAbnt:'ABNT formatting', tabReferences:'References', similarityTitle:'Similarity index', similarityText:'Similar excerpts account for {percent}% of the document.', referencesHeading:'Reviewed references', referenceValid:'Valid', referenceAttention:'Attention', reviewLoading:'Analyzing document…',
    themeDarkLabel:'Dark', themeLightLabel:'Light', themeDarkAction:'Switch to dark mode', themeLightAction:'Switch to light mode',
    invalidEmail:'Enter a valid email.', passwordTooShort:'Password must be at least 6 characters.', loginFailed:'Could not sign in. Check your credentials.',
    dashboardLoadFailed:'Could not sync your data.', reminderSaveFailed:'Could not save the reminder.', remindersLoadFailed:'Could not load the reminders.', reminderCreateFailed:'Could not create the reminder.',
    notesLoadFailed:'Could not load your notes.', noteSaveFailed:'Could not save the note.', folderCreateFailed:'Could not create the folder.',
    projectsLoadFailed:'Could not load your projects.', projectDetailsLoadFailed:'Could not load the project details.', stageSaveFailed:'Could not save the stage.',
    pomodoroLoadFailed:'Could not load your sessions.', reviewLoadFailed:'Could not load the review analysis.'
  }
}
const initial = localStorage.getItem('studyflow-locale') === 'en' ? 'en' : 'pt-BR'
const locale = ref<Locale>(initial)
export function useI18n() {
  const t = (key: string, params?: Record<string, string | number>) => {
    const template = messages[locale.value][key] ?? key
    if (!params) return template
    return template.replace(/\{(\w+)\}/g, (match, token: string) => (token in params ? String(params[token]) : match))
  }
  const toggleLocale = () => { locale.value = locale.value === 'pt-BR' ? 'en' : 'pt-BR'; localStorage.setItem('studyflow-locale', locale.value) }
  return { locale: computed(() => locale.value), t, toggleLocale }
}
