/* 诊断：打印 SSE 原始 chunk */
const BASE = process.argv[2] || 'http://localhost:8080'

async function main() {
  const res = await fetch(`${BASE}/api/ai/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message: '测试', history: [] })
  })
  console.log(`HTTP ${res.status}`)
  console.log('HEADERS:', Object.fromEntries(res.headers))
  const reader = res.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let chunkCount = 0, buf = '', full = ''
  outer: while (true) {
    const { value, done } = await reader.read()
    if (done) { console.log(`\n[STREAM DONE] chunks=${chunkCount}`); break }
    chunkCount++
    const text = decoder.decode(value, { stream: true })
    console.log(`[CHUNK ${chunkCount} ${value.length}B] ${JSON.stringify(text.slice(0, 120))}`)
    buf += text
    const parts = buf.split('\n\n')
    buf = parts.pop() || ''
    for (const part of parts) {
      for (const line of part.split('\n')) {
        const t = line.trim()
        if (!t.startsWith('data:')) continue
        const payload = t.slice(5).trim()
        if (payload === '[DONE]') { console.log('[GOT DONE]'); break outer }
        try {
          const parsed = JSON.parse(payload)
          const delta = typeof parsed === 'string' ? parsed : parsed?.choices?.[0]?.delta?.content
          if (delta) full += delta
          else console.log('[PARSED BUT NO DELTA]', JSON.stringify(parsed).slice(0, 80))
        } catch (e) { console.log('[PARSE FAIL]', JSON.stringify(payload.slice(0, 80)), e.message) }
      }
    }
    if (chunkCount > 20) break
  }
  console.log(`\n[PARSED FULL ${full.length}字] ${full.slice(0, 100)}`)
  process.exit(0)
}
main().catch(e => { console.log('ERR:', e.message); process.exit(1) })
