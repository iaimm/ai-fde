export interface Citation {
  src: string
  text: string
}

export interface Message {
  role: 'user' | 'assistant'
  content: string
  refs?: Citation[]
  activeRef?: number | null
}