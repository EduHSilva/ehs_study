export interface Note { id: string; folderId: string | null; title: string; content: string }
export interface NoteFolder { id: string; name: string; notes: Note[] }
