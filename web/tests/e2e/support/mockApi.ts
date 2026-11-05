import type { Page, Route } from '@playwright/test'

export interface Note {
  id: string
  folderId: string
  title: string
  content: string
}

export interface Folder {
  id: string
  name: string
  notes: Note[]
}

export interface DashboardPayload {
  weeklyProgress: { completed: number; total: number }
  tasks: unknown[]
  notes: Note[]
}

export interface MockState {
  folders: Folder[]
  dashboard?: DashboardPayload
  sessions?: unknown[]
  projects?: unknown[]
  projectDetails?: Record<string, unknown>
  nextProjectId?: number
  nextFolderId?: number
  nextNoteId?: number
}

const USER = {
  id: 'user-1',
  username: 'Ana Souza',
  email: 'ana@uni.br',
  lastLogin: null,
  token: null,
  course: 'História',
  avatarUrl: null
}

const DEFAULT_DASHBOARD: DashboardPayload = {
  weeklyProgress: { completed: 0, total: 0 },
  tasks: [],
  notes: []
}

function envelope(data: unknown) {
  return { message: 'ok', data, links: null }
}

async function fulfill(route: Route, data: unknown, status = 200) {
  await route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(envelope(data)) })
}

export async function signIn(page: Page) {
  await page.addInitScript(() => {
    localStorage.setItem(
      'studyflow-auth',
      JSON.stringify({
        token: 'test-token',
        user: { id: 'user-1', name: 'Ana Souza', email: 'ana@uni.br', course: 'História' }
      })
    )
    localStorage.setItem('studyflow-locale', 'pt-BR')
    localStorage.setItem('studyflow-theme', 'light')
  })
}

export async function mockApi(page: Page, state: MockState) {
  await page.route('**/users/auth/me', route => fulfill(route, USER))

  await page.route('**/users/auth/login', async route => {
    const body = route.request().postDataJSON() as { email?: string } | null
    await fulfill(route, { ...USER, email: body?.email ?? USER.email, token: 'test-token' })
  })

  await page.route('**/study/**', async route => {
    const request = route.request()
    const url = new URL(request.url())
    const path = url.pathname
    const method = request.method()

    if (path === '/study/dashboard' && method === 'GET') {
      return fulfill(route, state.dashboard ?? DEFAULT_DASHBOARD)
    }

    if (path === '/study/focus' && method === 'GET') {
      return fulfill(route, {
        total: state.sessions?.length ?? 0,
        page: 0,
        size: 100,
        items: state.sessions ?? []
      })
    }

    if (path === '/study/focus' && method === 'POST') {
      const body = request.postDataJSON() as { title: string; minutes: number }
      const session = { id: `session-${Date.now()}`, title: body.title, minutes: body.minutes, completedAt: new Date().toISOString() }
      state.sessions ??= []
      state.sessions.unshift(session)
      return fulfill(route, session)
    }

    if (path === '/study/projects' && method === 'GET') {
      return fulfill(route, {
        total: state.projects?.length ?? 0,
        page: 0,
        size: 100,
        items: state.projects ?? []
      })
    }

    if (path === '/study/projects/new' && method === 'POST') {
      const body = request.postDataJSON() as { title: string; status: string; advisor: string | null }
      const project = {
        id: `project-${state.nextProjectId ?? 10}`,
        title: body.title,
        status: body.status,
        progress: 0,
        advisor: body.advisor ?? '',
        stages: [],
        milestone: []
      }
      state.nextProjectId = (state.nextProjectId ?? 10) + 1
      state.projects ??= []
      state.projects.unshift(project)
      state.projectDetails ??= {}
      state.projectDetails[project.id] = project
      return fulfill(route, project)
    }

    if (path.startsWith('/study/projects/') && method === 'GET') {
      const id = path.split('/').pop()
      return fulfill(route, state.projectDetails?.[id ?? ''] ?? null)
    }

    const stageMatch = path.match(/^\/study\/project\/stage\/([^/]+)$/)
    if (stageMatch && method === 'PUT') {
      const stageId = stageMatch[1]
      const body = request.postDataJSON() as { idProject: string; title: string; done: boolean }
      const project = state.projectDetails?.[body.idProject] as { stages?: Array<{ id: string; name: string; done: boolean }> } | undefined
      const stage = project?.stages?.find(entry => entry.id === stageId)
      if (!stage) return fulfill(route, null, 404)
      Object.assign(stage, { name: body.title, done: body.done })
      return fulfill(route, stage)
    }

    if (path === '/study/project/stage' && method === 'POST') {
      const body = request.postDataJSON() as { idProject: string; title: string; done: boolean }
      const project = state.projectDetails?.[body.idProject] as { stages?: Array<{ id: string; name: string; done: boolean }> } | undefined
      if (!project) return fulfill(route, null, 404)
      project.stages ??= []
      const stage = { id: `stage-${Date.now()}`, name: body.title, done: body.done }
      project.stages.push(stage)
      return fulfill(route, stage)
    }

    const milestoneMatch = path.match(/^\/study\/project\/milestone\/([^/]+)$/)
    if (milestoneMatch && method === 'PUT') {
      const milestoneId = milestoneMatch[1]
      const body = request.postDataJSON() as { idProject: string; title: string; dueDate: string }
      const project = state.projectDetails?.[body.idProject] as { milestone?: Array<{ id: string; title: string; dueDate: string }> } | undefined
      const milestone = project?.milestone?.find(entry => entry.id === milestoneId)
      if (!milestone) return fulfill(route, null, 404)
      Object.assign(milestone, { title: body.title, dueDate: body.dueDate })
      return fulfill(route, milestone)
    }

    if (path === '/study/project/milestone' && method === 'POST') {
      const body = request.postDataJSON() as { idProject: string; title: string; dueDate: string }
      const project = state.projectDetails?.[body.idProject] as { milestone?: Array<{ id: string; title: string; dueDate: string }> } | undefined
      if (!project) return fulfill(route, null, 404)
      project.milestone ??= []
      const milestone = { id: `milestone-${Date.now()}`, title: body.title, dueDate: body.dueDate }
      project.milestone.push(milestone)
      return fulfill(route, milestone)
    }

    if (path === '/study/notes/folders' && method === 'GET') {
      return fulfill(route, { total: state.folders.length, page: 0, size: 100, items: state.folders })
    }

    if (path === '/study/notes/folders' && method === 'POST') {
      const body = request.postDataJSON() as { name: string }
      const folder: Folder = {
        id: `folder-${state.nextFolderId ?? 10}`,
        name: body.name,
        notes: []
      }
      state.nextFolderId = (state.nextFolderId ?? 10) + 1
      state.folders.push(folder)
      return fulfill(route, folder)
    }

    if (path.startsWith('/study/notes/folders/') && method === 'PUT') {
      const id = path.split('/').pop()
      const body = request.postDataJSON() as { name: string }
      const folder = state.folders.find(item => item.id === id)
      if (folder) folder.name = body.name
      return fulfill(route, folder ?? null)
    }

    if (path.startsWith('/study/notes/folders/') && method === 'DELETE') {
      const id = path.split('/').pop()
      state.folders.splice(0, state.folders.length, ...state.folders.filter(folder => folder.id !== id))
      return fulfill(route, null)
    }

    if (path.startsWith('/study/notes/') && method === 'POST') {
      const folderId = path.split('/').pop()
      const folder = state.folders.find(item => item.id === folderId)
      if (!folder) return fulfill(route, null, 404)
      const body = request.postDataJSON() as { title: string; content: string }
      const note: Note = {
        id: `note-${state.nextNoteId ?? 10}`,
        folderId: folder.id,
        title: body.title,
        content: body.content
      }
      state.nextNoteId = (state.nextNoteId ?? 10) + 1
      folder.notes.push(note)
      return fulfill(route, note)
    }

    if (path.startsWith('/study/notes/') && method === 'PUT') {
      const id = path.split('/').pop()
      const body = request.postDataJSON() as { title: string; content: string }
      const note = state.folders.flatMap(folder => folder.notes).find(item => item.id === id)
      if (note) {
        note.title = body.title
        note.content = body.content
      }
      return fulfill(route, note ?? null)
    }

    if (path.startsWith('/study/notes/') && method === 'DELETE') {
      const id = path.split('/').pop()
      state.folders.forEach(folder => {
        folder.notes.splice(0, folder.notes.length, ...folder.notes.filter(note => note.id !== id))
      })
      return fulfill(route, null)
    }

    return route.fulfill({
      status: 404,
      contentType: 'application/json',
      body: JSON.stringify({ message: `unmocked ${method} ${path}` })
    })
  })
}
