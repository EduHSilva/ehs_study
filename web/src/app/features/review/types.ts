export interface AbntCheck { label: string; passed: boolean }
export interface ReferenceCheck { citation: string; status: 'Válida' | 'Atenção' }
export interface ReviewReport {
  documentTitle: string
  excerpt: string
  flaggedExcerpt: string
  similarityPercent: number
  abntChecks: AbntCheck[]
  references: ReferenceCheck[]
}
