// 授课 API 客户端：统一 fetch，失败抛错。关联：types、ChatPanel、QuizCard、ProviderSettings。
import type { AnswerResult, ApiResponse, MotifSession, ProviderSettings } from '../types'

/** 解析 JSON；success 为 false 时抛错 */
async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: { 'Content-Type': 'application/json', ...(init?.headers ?? {}) },
    ...init,
  })
  let body: ApiResponse<T>
  try {
    body = (await res.json()) as ApiResponse<T>
  } catch {
    throw new Error(`请求失败（HTTP ${res.status}）`)
  }
  if (!body.success) {
    throw new Error(body.message || `请求失败（HTTP ${res.status}）`)
  }
  return body.data
}

/** 开始一课 */
export function startLesson(concept: string): Promise<MotifSession> {
  return request<MotifSession>('/api/lessons', {
    method: 'POST',
    body: JSON.stringify({ concept }),
  })
}

/** 按 id 取会话 */
export function getLesson(id: string): Promise<MotifSession> {
  return request<MotifSession>(`/api/lessons/${id}`)
}

/** 升简版等级 */
export function simplify(id: string): Promise<MotifSession> {
  return request<MotifSession>(`/api/lessons/${id}/simplify`, { method: 'POST' })
}

/** 换故事重写 */
export function rewrite(id: string): Promise<MotifSession> {
  return request<MotifSession>(`/api/lessons/${id}/rewrite`, { method: 'POST' })
}

/** 提交检验题答案 */
export function answer(
  id: string,
  quizId: string,
  choiceIndex: number,
): Promise<AnswerResult> {
  return request<AnswerResult>(`/api/lessons/${id}/answer`, {
    method: 'POST',
    body: JSON.stringify({ quizId, choiceIndex }),
  })
}

/** 读取 Provider 设置（apiKey 已脱敏） */
export function getProvider(): Promise<ProviderSettings> {
  return request<ProviderSettings>('/api/provider')
}

/** 保存 Provider；apiKey 空字符串表示保留原密钥 */
export function saveProvider(body: ProviderSettings): Promise<ProviderSettings> {
  return request<ProviderSettings>('/api/provider', {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}
