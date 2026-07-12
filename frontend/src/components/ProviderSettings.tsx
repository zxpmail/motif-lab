// Provider 设置表单：baseUrl / model / apiKey / enabled。关联：App、api/client。
import { FormEvent, useEffect, useState } from 'react'
import { getProvider, saveProvider } from '../api/client'
import type { ProviderSettings as ProviderSettingsType } from '../types'

interface ProviderSettingsProps {
  onClose: () => void
}

/** 最小 Provider 设置页；apiKey 留空表示不改原密钥 */
export default function ProviderSettings({ onClose }: ProviderSettingsProps) {
  const [baseUrl, setBaseUrl] = useState('')
  const [model, setModel] = useState('')
  const [apiKey, setApiKey] = useState('')
  const [maskedKey, setMaskedKey] = useState<string | null>(null)
  const [enabled, setEnabled] = useState(false)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      try {
        const p = await getProvider()
        if (cancelled) return
        applyDto(p)
      } catch (e) {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : '加载失败')
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [])

  function applyDto(p: ProviderSettingsType) {
    setBaseUrl(p.baseUrl ?? '')
    setModel(p.model ?? '')
    setMaskedKey(p.apiKey)
    setApiKey('')
    setEnabled(!!p.enabled)
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    setSaved(false)
    try {
      const next = await saveProvider({
        baseUrl,
        model,
        apiKey: apiKey.trim() ? apiKey.trim() : '',
        enabled,
      })
      applyDto(next)
      setSaved(true)
    } catch (err) {
      setError(err instanceof Error ? err.message : '保存失败')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50 text-gray-900 p-6 max-w-xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">Provider 设置</h1>
        <button
          type="button"
          onClick={onClose}
          className="text-lg px-4 py-2 rounded-lg border border-gray-300 bg-white hover:bg-gray-50"
        >
          返回
        </button>
      </div>

      {loading ? (
        <p className="text-gray-500">加载中…</p>
      ) : (
        <form onSubmit={onSubmit} className="space-y-4">
          <p className="text-base text-gray-600 leading-relaxed">
            本应用只支持 OpenAI 兼容接口。DeepSeek 请填
            <code className="mx-1 px-1 bg-gray-100 rounded">https://api.deepseek.com/v1</code>
            ，不要用 Tepeu 里的 <code className="mx-1 px-1 bg-gray-100 rounded">/anthropic</code> 地址。
            循环/变量等金牌课不走模型；只有新概念或「换个故事」才用这里的配置。
          </p>

          <label className="block space-y-1">
            <span className="text-lg">baseUrl</span>
            <input
              value={baseUrl}
              onChange={(e) => setBaseUrl(e.target.value)}
              className="w-full text-lg px-3 py-2 rounded-lg border border-gray-300"
              placeholder="https://api.deepseek.com/v1"
            />
          </label>

          <label className="block space-y-1">
            <span className="text-lg">model</span>
            <input
              value={model}
              onChange={(e) => setModel(e.target.value)}
              className="w-full text-lg px-3 py-2 rounded-lg border border-gray-300"
              placeholder="deepseek-chat"
            />
          </label>

          <label className="block space-y-1">
            <span className="text-lg">apiKey（留空则保留原密钥）</span>
            {maskedKey && (
              <span className="block text-sm text-gray-500">当前：{maskedKey}</span>
            )}
            <input
              type="password"
              value={apiKey}
              onChange={(e) => setApiKey(e.target.value)}
              className="w-full text-lg px-3 py-2 rounded-lg border border-gray-300"
              placeholder="sk-…"
              autoComplete="off"
            />
          </label>

          <label className="flex items-center gap-3 text-lg">
            <input
              type="checkbox"
              checked={enabled}
              onChange={(e) => setEnabled(e.target.checked)}
              className="w-5 h-5"
            />
            启用
          </label>

          <button
            type="submit"
            disabled={saving}
            className="text-lg px-5 py-3 rounded-lg bg-gray-900 text-white disabled:opacity-50"
          >
            {saving ? '保存中…' : '保存'}
          </button>

          {saved && <p className="text-green-700">已保存</p>}
          {error && <p className="text-red-600">{error}</p>}
        </form>
      )}
    </div>
  )
}
