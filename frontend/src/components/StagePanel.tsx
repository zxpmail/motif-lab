// 右侧舞台：分镜列表 + sandbox iframe。关联：LessonLayout、types。
import type { MotifSession } from '../types'

interface StagePanelProps {
  session: MotifSession | null
}

/** 渲染分镜骨架；有 demoUrl 则 iframe 播放；DEMO 且无 url 显示生成中 */
export default function StagePanel({ session }: StagePanelProps) {
  if (!session) {
    return (
      <div className="h-full flex items-center justify-center text-xl text-gray-500 p-8">
        输入概念后，这里会先出现分镜骨架
      </div>
    )
  }

  const { storyboard, demoUrl, phase } = session
  const generating = phase === 'DEMO' && !demoUrl

  return (
    <div className="h-full flex flex-col gap-4 p-6 overflow-auto">
      {storyboard && (
        <section aria-label="分镜骨架" className="space-y-3">
          <h2 className="text-2xl font-semibold text-gray-900">{storyboard.title}</h2>
          <ol className="space-y-3 list-decimal list-inside text-lg text-gray-800">
            {storyboard.beats.map((beat, i) => (
              <li key={i} className="pl-1">
                <span className="font-medium">{beat.who}</span>
                ：{beat.action}
                <span className="text-gray-600"> → {beat.result}</span>
                <span className="block text-base text-gray-500 mt-0.5">
                  原理：{beat.principle}
                </span>
              </li>
            ))}
          </ol>
        </section>
      )}

      {generating && (
        <p className="text-xl text-amber-700 animate-pulse">动画生成中…</p>
      )}

      {demoUrl && (
        <div className="flex-1 min-h-[280px] border border-gray-200 rounded-lg overflow-hidden bg-white">
          <iframe
            sandbox="allow-scripts"
            src={demoUrl}
            title="demo"
            className="w-full h-full min-h-[280px] border-0"
          />
        </div>
      )}
    </div>
  )
}
