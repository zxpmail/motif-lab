// 左侧对话：开课、简化、换故事、口诀与检验题。关联：api/client、QuizCard、LessonLayout。
import { FormEvent, useState } from 'react'
import { rewrite, simplify, startLesson } from '../api/client'
import type { MotifSession } from '../types'
import QuizCard from './QuizCard'

interface ChatPanelProps {
  session: MotifSession | null
  onSessionUpdate: (session: MotifSession) => void
}

/** 概念输入与操作按钮；有会话时展示口诀与 QuizCard */
export default function ChatPanel({ session, onSessionUpdate }: ChatPanelProps) {
  const [concept, setConcept] = useState('循环')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  /** 统一执行异步操作并回写会话 */
  async function run(action: () => Promise<MotifSession>) {
    setLoading(true)
    setError(null)
    try {
      const next = await action()
      onSessionUpdate(next)
    } catch (e) {
      setError(e instanceof Error ? e.message : '操作失败')
    } finally {
      setLoading(false)
    }
  }

  /** 提交概念开课 */
  function onSubmit(e: FormEvent) {
    e.preventDefault()
    const trimmed = concept.trim()
    if (!trimmed) return
    void run(() => startLesson(trimmed))
  }

  const showQuiz =
    session &&
    (session.phase === 'MOTTO_QUIZ' || session.phase === 'DONE') &&
    session.quiz?.length > 0

  return (
    <div className="h-full flex flex-col gap-6 p-6 overflow-auto">
      <header>
        <h1 className="text-3xl font-bold text-gray-900">授课</h1>
        <p className="mt-1 text-lg text-gray-600">先母题寓言，再口诀检验（暂不生成动画）</p>
      </header>

      <form onSubmit={onSubmit} className="space-y-3">
        <label className="block text-lg text-gray-800" htmlFor="concept-input">
          想学什么概念？
        </label>
        <div className="flex flex-wrap gap-2">
          <input
            id="concept-input"
            value={concept}
            onChange={(e) => setConcept(e.target.value)}
            className="flex-1 min-w-[8rem] text-xl px-4 py-3 rounded-lg border border-gray-300"
            placeholder="例如：循环"
            disabled={loading}
          />
          <button
            type="submit"
            disabled={loading}
            className="text-xl px-5 py-3 rounded-lg bg-gray-900 text-white disabled:opacity-50"
          >
            开始
          </button>
        </div>
        <div className="flex flex-wrap gap-2">
          {['循环', '变量', '条件', '函数'].map((name) => (
            <button
              key={name}
              type="button"
              disabled={loading}
              onClick={() => {
                setConcept(name)
                void run(() => startLesson(name))
              }}
              className="text-lg px-4 py-2 rounded-lg border border-gray-300 bg-white hover:bg-gray-50 disabled:opacity-50"
            >
              {name}
            </button>
          ))}
        </div>
      </form>

      {session && (
        <div className="space-y-3">
          <p className="text-base text-gray-500">
            当前：{session.conceptRaw} · 简版 L{session.level} · {session.phase}
          </p>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              disabled={loading}
              onClick={() => void run(() => simplify(session.id))}
              className="text-lg px-4 py-2 rounded-lg border border-gray-300 bg-white hover:bg-gray-50 disabled:opacity-50"
            >
              不懂 / 更简单
            </button>
            <button
              type="button"
              disabled={loading}
              onClick={() => void run(() => rewrite(session.id))}
              className="text-lg px-4 py-2 rounded-lg border border-gray-300 bg-white hover:bg-gray-50 disabled:opacity-50"
            >
              换个故事（/重写）
            </button>
          </div>
        </div>
      )}

      {session?.motto && (
        <blockquote className="text-xl text-gray-900 border-l-4 border-gray-400 pl-4 py-1">
          {session.motto}
        </blockquote>
      )}

      {showQuiz && session && (
        <QuizCard
          sessionId={session.id}
          quiz={session.quiz}
          correctQuizIds={session.correctQuizIds ?? []}
          onSessionUpdate={onSessionUpdate}
        />
      )}

      {session?.phase === 'DONE' && (
        <p className="text-xl text-green-700">本课完成</p>
      )}

      {error && <p className="text-base text-red-600">{error}</p>}
      {loading && <p className="text-base text-gray-500">处理中…</p>}
    </div>
  )
}
