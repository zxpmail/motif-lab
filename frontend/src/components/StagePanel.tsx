// 右侧：母题寓言 + 对照表 + 提炼（不再播动画）。关联：LessonLayout、types。
import type { MotifSession } from '../types'

interface StagePanelProps {
  session: MotifSession | null
}

/** 展示母题文字；生成中提示等待 */
export default function StagePanel({ session }: StagePanelProps) {
  if (!session) {
    return (
      <div className="h-full flex items-center justify-center text-xl text-gray-500 p-8">
        输入概念后，这里会出现母题寓言
      </div>
    )
  }

  const { storyboard, fable, explanation, contrast, motif, phase, error } = session
  const generating = phase === 'DEMO' && !fable && !error

  return (
    <div className="h-full flex flex-col gap-5 p-6 overflow-auto">
      {storyboard && (
        <h2 className="text-2xl font-semibold text-gray-900">{storyboard.title}</h2>
      )}

      {generating && (
        <p className="text-xl text-amber-700 animate-pulse">
          正在写母题寓言（通常十几秒到一分钟）…
        </p>
      )}

      {error && (
        <p className="text-lg text-red-600" role="alert">
          {error}
        </p>
      )}

      {fable && (
        <section aria-label="母题寓言" className="space-y-2">
          <h3 className="text-lg font-medium text-gray-700">寓言</h3>
          <p className="text-xl leading-relaxed text-gray-900 whitespace-pre-wrap">{fable}</p>
        </section>
      )}

      {explanation && (
        <section aria-label="概念解释" className="space-y-2">
          <h3 className="text-lg font-medium text-gray-700">这是在讲什么</h3>
          <p className="text-lg text-gray-800 whitespace-pre-wrap">{explanation}</p>
        </section>
      )}

      {contrast && contrast.length > 0 && (
        <section aria-label="对照表" className="space-y-2">
          <h3 className="text-lg font-medium text-gray-700">对照</h3>
          <table className="w-full text-left text-lg border-collapse">
            <thead>
              <tr className="border-b border-gray-300">
                <th className="py-2 pr-3 font-medium">故事里</th>
                <th className="py-2 font-medium">概念里</th>
              </tr>
            </thead>
            <tbody>
              {contrast.map((row, i) => (
                <tr key={i} className="border-b border-gray-100 align-top">
                  <td className="py-2 pr-3 text-gray-800">{row.story}</td>
                  <td className="py-2 text-gray-800">{row.concept}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}

      {motif && (
        <section aria-label="母题提炼" className="space-y-2">
          <h3 className="text-lg font-medium text-gray-700">母题</h3>
          <p className="text-xl font-semibold text-gray-900 border-l-4 border-amber-500 pl-4">
            {motif}
          </p>
        </section>
      )}
    </div>
  )
}
