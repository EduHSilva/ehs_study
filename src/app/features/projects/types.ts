export type ProjectStatus = 'Em andamento' | 'Concluído'

export interface Project { id: string; title: string; progress: number; status: ProjectStatus; advisor: string }
export interface ProjectStage { id: string; name: string; done: boolean }
export interface ProjectMilestone { id: string; title: string; dueDate: string }
export interface ProjectDetails { project: Project; stages: ProjectStage[]; milestones: ProjectMilestone[] }
