// 母题会话与分镜/检验题前端类型。关联：api/client、各 Lesson 组件。

/** 分镜节拍：谁、动作、结果、原理点 */
export interface Beat {
  who: string
  action: string
  result: string
  principle: string
}

/** 分镜骨架 */
export interface Storyboard {
  conceptId: string
  title: string
  beats: Beat[]
}

/** 检验题条目（answerIndex 仅后端权威，前端可选携带） */
export interface QuizItem {
  id: string
  question: string
  choices: string[]
  answerIndex?: number
}

/** 会话阶段 */
export type LessonPhase = 'STORYBOARD' | 'DEMO' | 'MOTTO_QUIZ' | 'DONE' | string

/** 母题授课会话（对齐后端 MotifSession JSON） */
export interface MotifSession {
  id: string
  conceptRaw: string
  conceptId: string
  level: number
  sceneSeed: string | null
  storyboard: Storyboard
  demoUrl: string | null
  motto: string | null
  quiz: QuizItem[]
  phase: LessonPhase
  correctQuizIds: string[]
  /** 异步生成失败时的可读错误 */
  error?: string | null
}

/** LLM Provider 设置（GET 时 apiKey 为脱敏值） */
export interface ProviderSettings {
  apiKey: string | null
  baseUrl: string
  model: string
  enabled: boolean
}

/** 统一 API 包装 */
export interface ApiResponse<T> {
  success: boolean
  data: T
  message?: string | null
}

/** 答题接口返回 */
export interface AnswerResult {
  correct: boolean
  session: MotifSession
}
