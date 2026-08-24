export interface NoteFolder { id: string; name: string }
export interface Note { id: string; folderId: string; title: string; content: string }
export interface NotesIndex { folders: NoteFolder[]; notes: Note[] }
