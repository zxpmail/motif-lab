// 根组件：顶栏「设置」+ 授课布局或 Provider 设置页。关联：LessonLayout、ProviderSettings。
import { useState } from 'react'
import LessonLayout from './components/LessonLayout'
import ProviderSettings from './components/ProviderSettings'

export default function App() {
  const [showSettings, setShowSettings] = useState(false)

  if (showSettings) {
    return <ProviderSettings onClose={() => setShowSettings(false)} />
  }

  return (
    <div className="min-h-screen flex flex-col">
      <header className="flex items-center justify-between px-4 py-2 border-b border-gray-200 bg-white shrink-0">
        <span className="text-lg font-semibold text-gray-900">母题实验室</span>
        <button
          type="button"
          onClick={() => setShowSettings(true)}
          className="text-base px-3 py-1.5 rounded-lg border border-gray-300 bg-white hover:bg-gray-50"
        >
          设置
        </button>
      </header>
      <div className="flex-1 min-h-0">
        <LessonLayout />
      </div>
    </div>
  )
}
