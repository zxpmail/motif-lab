// 左右两栏布局：左对话 / 右舞台；DEMO 无 url 时轮询会话。关联：ChatPanel、StagePanel、App。
import { useEffect, useState } from 'react'
import { getLesson } from '../api/client'
import type { MotifSession } from '../types'
import ChatPanel from './ChatPanel'
import StagePanel from './StagePanel'

/** 大字号极简两栏：会话状态提升到此层；生成中每 1.5s 拉取一次 */
export default function LessonLayout() {
  const [session, setSession] = useState<MotifSession | null>(null)

  useEffect(() => {
    if (!session) return
    // 生成母题中：phase=DEMO 且尚无寓言
    if (session.phase !== 'DEMO' || session.fable || session.error) return

    const id = session.id
    const timer = window.setInterval(() => {
      void getLesson(id)
        .then((next) => setSession(next))
        .catch(() => {
          /* 轮询失败时保持现状，下一拍再试 */
        })
    }, 1500)

    return () => window.clearInterval(timer)
  }, [session?.id, session?.phase, session?.fable, session?.error])

  return (
    <div className="h-full min-h-[calc(100vh-3rem)] grid grid-cols-1 md:grid-cols-2 bg-gray-50 text-gray-900">
      <aside className="border-b md:border-b-0 md:border-r border-gray-200 bg-white min-h-[50vh] md:min-h-full">
        <ChatPanel session={session} onSessionUpdate={setSession} />
      </aside>
      <main className="min-h-[50vh] md:min-h-full bg-gray-50 flex flex-col">
        {session?.error && (
          <p className="mx-6 mt-4 text-lg text-red-600 shrink-0" role="alert">
            {session.error}
          </p>
        )}
        <StagePanel session={session} />
      </main>
    </div>
  )
}
