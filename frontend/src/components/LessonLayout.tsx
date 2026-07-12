// 左右两栏布局：左对话 / 右舞台。关联：ChatPanel、StagePanel、App。
import { useState } from 'react'
import type { MotifSession } from '../types'
import ChatPanel from './ChatPanel'
import StagePanel from './StagePanel'

/** 大字号极简两栏：会话状态提升到此层 */
export default function LessonLayout() {
  const [session, setSession] = useState<MotifSession | null>(null)

  return (
    <div className="min-h-screen grid grid-cols-1 md:grid-cols-2 bg-gray-50 text-gray-900">
      <aside className="border-b md:border-b-0 md:border-r border-gray-200 bg-white min-h-[50vh] md:min-h-screen">
        <ChatPanel session={session} onSessionUpdate={setSession} />
      </aside>
      <main className="min-h-[50vh] md:min-h-screen bg-gray-50">
        <StagePanel session={session} />
      </main>
    </div>
  )
}
