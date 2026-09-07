/* 模拟浏览器 fetch SSE 调用（Node fetch 与浏览器实现一致） */
const BASE = process.argv[2] || 'http://localhost:3000'

async function test(msg) {
  console.log(`\n>>> 提问: ${msg}`)
  try {
    const res = await fetch(`${BASE}/api/ai/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: msg, history: [] })
    })
    console.log(`HTTP ${res.status} ${res.headers.get('content-type')}`)
    const reader = res.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buf = '', full = '', done = false
    while (!done) {
      const { value, done: d } = await reader.read()
      if (d) break
      buf += decoder.decode(value, { stream: true })
      const parts = buf.split('\n\n')
      buf = parts.pop() || ''
      for (const part of parts) {
        for (const line of part.split('\n')) {
          const t = line.trim()
          if (!t.startsWith('data:')) continue
          const payload = t.slice(5).trim()
          if (payload === '[DONE]') { done = true; break }
          try {
            const parsed = JSON.parse(payload)
            const delta = typeof parsed === 'string' ? parsed : parsed?.choices?.[0]?.delta?.content
            if (delta) full += delta
          } catch (e) {}
        }
        if (done) break
      }
    }
    console.log(`回复(${full.length}字): ${full.slice(0, 120)}...`)
    return full.length > 0
  } catch (e) {
    console.log(`FETCH ERROR: ${e.message}`)
    return false
  }
}

;(async () => {
  const ok1 = await test('今日销售额是多少')
  const ok2 = await test('西安今日销售多少？')
  console.log(`\n===== 结果: ${ok1 && ok2 ? 'PASS ✓ 前端SSE解析逻辑正常' : 'FAIL ✗'} =====`)
  process.exit(ok1 && ok2 ? 0 : 1)
})()
