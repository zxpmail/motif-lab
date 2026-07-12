// 检验题卡片：选题后调 answer API。关联：api/client、ChatPanel、types。
import { useState } from 'react'
import { answer } from '../api/client'
import type { MotifSession, QuizItem } from '../types'

interface QuizCardProps {
  sessionId: string
  quiz: QuizItem[]
  correctQuizIds: string[]
  onSessionUpdate: (session: MotifSession) => void
}

/** 列出题目并处理作答反馈 */
export default function QuizCard({
  sessionId,
  quiz,
  correctQuizIds,
  onSessionUpdate,
}: QuizCardProps) {
  const [busyId, setBusyId] = useState<string | null>(null)
  const [feedback, setFeedback] = useState<Record<string, 'ok' | 'bad'>>({})
  const [error, setError] = useState<string | null>(null)

  /** 选择某个选项并提交 */
  async function onChoose(quizId: string, choiceIndex: number) {
    if (correctQuizIds.includes(quizId) || busyId) return
    setBusyId(quizId)
    setError(null)
    try {
      const result = await answer(sessionId, quizId, choiceIndex)
      setFeedback((prev) => ({
        ...prev,
        [quizId]: result.correct ? 'ok' : 'bad',
      }))
      onSessionUpdate(result.session)
    } catch (e) {
      setError(e instanceof Error ? e.message : '答题失败')
    } finally {
      setBusyId(null)
    }
  }

  if (!quiz.length) return null

  return (
    <section className="space-y-4" aria-label="检验题">
      <h2 className="text-xl font-semibold text-gray-900">检验一下</h2>
      {quiz.map((item) => {
        const done = correctQuizIds.includes(item.id)
        const mark = feedback[item.id]
        return (
          <div key={item.id} className="space-y-2">
            <p className="text-lg text-gray-800">{item.question}</p>
            <ul className="space-y-2">
              {item.choices.map((choice, index) => (
                <li key={`${item.id}-${index}`}>
                  <button
                    type="button"
                    disabled={done || busyId === item.id}
                    onClick={() => void onChoose(item.id, index)}
                    className="w-full text-left text-lg px-4 py-3 rounded-lg border border-gray-300 bg-white hover:bg-gray-50 disabled:opacity-60 disabled:cursor-default"
                  >
                    {choice}
                  </button>
                </li>
              ))}
            </ul>
            {done && (
              <p className="text-base text-green-700">答对了</p>
            )}
            {!done && mark === 'bad' && (
              <p className="text-base text-amber-700">再想想，可以选「更简单」再看一遍</p>
            )}
          </div>
        )
      })}
      {error && <p className="text-base text-red-600">{error}</p>}
    </section>
  )
}
