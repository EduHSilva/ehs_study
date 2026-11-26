export type ProjectStatus = 'Pendente' | 'Em andamento' | 'Concluído'

export interface Project { id: string; title: string; progress: number; status: ProjectStatus; advisor: string; link: string }
export interface ProjectStage { id: string; name: string; done: boolean; projectProgress?: number }
export interface ProjectVersion { id: string; title: string; dueDate: string }
export interface ProjectDetails { project: Project; stages: ProjectStage[]; versions: ProjectVersion[] }
