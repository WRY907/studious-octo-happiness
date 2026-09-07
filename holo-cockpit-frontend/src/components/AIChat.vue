<template>
  <!-- 右下角浮动按钮 -->
  <button class="ai-fab" :class="{ open: visible }" title="AI 数据分析师" @click="togglePanel">
    <span class="fab-icon">🤖</span>
  </button>

  <!-- 对话窗 360×480 -->
  <transition name="ai-pop">
    <div v-if="visible" class="ai-panel">
      <header class="ai-header">
        <span class="ai-avatar">🤖</span>
        <div class="ai-title">
          <span class="t-cn">AI 数据分析师</span>
          <span class="t-en font-data">DOUBAO MODEL</span>
        </div>
        <button class="ai-close" title="关闭" @click="closePanel">✕</button>
      </header>

      <!-- 消息列表 -->
      <div ref="msgBox" class="ai-messages">
        <div
          v-for="(m, i) in messages"
          :key="i"
          class="msg-row"
          :class="m.role"
        >
          <div class="msg-bubble">
            <span class="msg-text">{{ m.content }}</span>
            <!-- 流式输出时的打字光标 -->
            <span v-if="streaming && m.role === 'assistant' && i === messages.length - 1" class="type-cursor"></span>
          </div>
        </div>
        <!-- 等待响应 -->
        <div v-if="sending" class="msg-row assistant">
          <div class="msg-bubble"><span class="dot-flash">● ● ●</span></div>
        </div>
      </div>

      <!-- 快捷问题 -->
      <div class="ai-quick">
        <button
          v-for="q in QUICK_QUESTIONS"
          :key="q"
          class="quick-btn"
          :disabled="busy"
          @click="send(q)"
        >{{ q }}</button>
      </div>

      <!-- 输入区 -->
      <div class="ai-input-row">
        <input
          v-model="inputText"
          class="ai-input"
          type="text"
          placeholder="输入问题，Enter 发送…"
          :disabled="busy"
          @keydown.enter.exact.prevent="send()"
        />
        <button class="ai-send" :disabled="busy || !inputText.trim()" @click="send()" title="发送">
          <span v-if="busy" class="send-spin"></span>
          <span v-else>➤</span>
        </button>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, onMounted } from 'vue'
import { aiApi } from '@/api'

const props = defineProps({ open: { type: Boolean, default: false } })
const emit = defineEmits(['close'])

const visible = ref(false)
const messages = ref([])
const inputText = ref('')
const sending = ref(false)   // 等待响应
const streaming = ref(false) // SSE 流式接收中
const msgBox = ref(null)

const QUICK_QUESTIONS = ['上周哪个省份卖得最好？', '西安今日销售多少？', '生成销售建议']
const busy = computed(() => sending.value || streaming.value)

/* 父级（顶栏 AI 按钮）打开 */
watch(() => props.open, v => { if (v) visible.value = true })

function togglePanel() {
  visible.value = !visible.value
  if (!visible.value) emit('close')
}
function closePanel() {
  visible.value = false
  emit('close')
}

/* 初始欢迎语 */
onMounted(() => {
  messages.value.push({
    role: 'assistant',
    content: '您好，我是 AI 数据分析师（豆包大模型驱动）。可以问我销售数据、区域表现、热销机型，也可以让我生成经营建议。'
  })
})

function scrollToBottom() {
  nextTick(() => {
    if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
  })
}

/* 发送消息：SSE 流式读取 */
async function send(preset) {
  const text = (preset ?? inputText.value).trim()
  if (!text || busy.value) return
  inputText.value = ''
  messages.value.push({ role: 'user', content: text })
  scrollToBottom()

  sending.value = true
  // assistant 占位（reactive 以便流式追加触发更新）
  const aiMsg = reactive({ role: 'assistant', content: '' })
  messages.value.push(aiMsg)
  scrollToBottom()

  try {
    // history 取最近 6 轮（12 条）
    const history = messages.value
      .slice(0, -2)
      .slice(-12)
      .map(m => ({ role: m.role, content: m.content }))

    const res = await aiApi.chat(text, history)
    if (!res.ok) throw new Error('HTTP ' + res.status)

    sending.value = false
    streaming.value = true

    const reader = res.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buf = ''
    let finished = false
    while (!finished) {
      const { value, done } = await reader.read()
      if (done) break
      buf += decoder.decode(value, { stream: true })
      // 按 \n\n 分割 SSE 事件，保留未完成的尾部
      const parts = buf.split('\n\n')
      buf = parts.pop() || ''
      for (const part of parts) {
          for (const line of part.split('\n')) {
            const t = line.trim()
            if (!t.startsWith('data:')) continue
            const payload = t.slice(5).trim()
            if (payload === '[DONE]') { finished = true; break }
            try {
              const parsed = JSON.parse(payload)
              // 兼容两种后端格式：纯文本片段（JSON字符串）或 OpenAI 分片格式
              const delta = typeof parsed === 'string'
                ? parsed
                : parsed?.choices?.[0]?.delta?.content
              if (delta) {
                aiMsg.content += delta
                scrollToBottom()
              }
            } catch (e) { /* 忽略无法解析的行 */ }
          }
          if (finished) break
        }
    }
    if (!aiMsg.content) aiMsg.content = '（AI 未返回内容，请重试）'
  } catch (e) {
    aiMsg.content = 'AI 服务暂不可用，请稍后重试。'
  } finally {
    sending.value = false
    streaming.value = false
    scrollToBottom()
  }
}
</script>

<style scoped>
/* ===== 浮动按钮 ===== */
.ai-fab {
  position: fixed;
  right: 24px;
  bottom: 80px;
  width: 54px;
  height: 54px;
  border-radius: 50%;
  border: 1px solid rgba(0, 255, 163, 0.5);
  background: linear-gradient(135deg, rgba(0, 60, 90, 0.9), rgba(0, 30, 60, 0.95));
  box-shadow: 0 0 22px rgba(0, 229, 255, 0.35), 0 4px 16px rgba(0, 0, 0, 0.5);
  cursor: pointer;
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform var(--dur-fast), box-shadow var(--dur-fast);
  animation: fab-float 3.5s ease-in-out infinite;
}
.ai-fab:hover { transform: scale(1.1); box-shadow: 0 0 30px rgba(0, 229, 255, 0.55); }
.ai-fab.open { animation: none; border-color: rgba(0, 229, 255, 0.7); }
/* 呼吸光晕（叠加在浮动动画之外，用 ::after 避免动画属性冲突） */
.ai-fab::after {
  content: '';
  position: absolute;
  inset: -5px;
  border-radius: 50%;
  border: 1px solid rgba(0, 255, 163, 0.5);
  opacity: 0;
  pointer-events: none;
  animation: fab-breathe 3.5s ease-in-out infinite;
}
@keyframes fab-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}
@keyframes fab-breathe {
  0%, 100% { opacity: 0; transform: scale(0.96); }
  50% { opacity: 0.85; transform: scale(1.14); }
}
.fab-icon { font-size: 26px; }

/* ===== 对话窗 ===== */
.ai-panel {
  position: fixed;
  right: 24px;
  bottom: 146px;
  width: 360px;
  height: 480px;
  z-index: 999;
  display: flex;
  flex-direction: column;
  background: rgba(4, 16, 40, 0.92);
  border: 1px solid rgba(0, 229, 255, 0.35);
  border-radius: 8px;
  backdrop-filter: blur(14px);
  box-shadow: 0 0 34px rgba(0, 165, 255, 0.25), 0 12px 40px rgba(0, 0, 0, 0.55);
  overflow: hidden;
}

/* 弹入弹出（仅 transform/opacity） */
.ai-pop-enter-active { transition: opacity 0.28s ease, transform 0.28s var(--ease-out); }
.ai-pop-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.ai-pop-enter-from,
.ai-pop-leave-to { opacity: 0; transform: translateY(18px) scale(0.96); }

/* ===== 头部 ===== */
.ai-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: linear-gradient(90deg, rgba(0, 100, 160, 0.35), rgba(0, 40, 90, 0.2));
  border-bottom: 1px solid rgba(0, 229, 255, 0.25);
}
.ai-avatar { font-size: 22px; }
.ai-title { flex: 1; display: flex; flex-direction: column; line-height: 1.25; }
.t-cn { font-size: 13px; font-weight: 600; color: var(--text-primary); letter-spacing: 1px; }
.t-en { font-size: 9px; font-weight: 600; letter-spacing: 2px; color: var(--text-faint); }
.ai-close {
  background: transparent;
  border: none;
  color: var(--text-muted);
  font-size: 14px;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 3px;
  transition: all var(--dur-fast);
}
.ai-close:hover { color: #fff; background: rgba(255, 77, 106, 0.25); }

/* ===== 消息列表 ===== */
.ai-messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.msg-row { display: flex; }
.msg-row.user { justify-content: flex-end; }
.msg-row.assistant { justify-content: flex-start; }

.msg-bubble {
  max-width: 82%;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 12.5px;
  line-height: 1.65;
  word-break: break-word;
  white-space: pre-wrap;
}
.msg-row.user .msg-bubble {
  background: linear-gradient(135deg, rgba(0, 229, 255, 0.22), rgba(0, 165, 255, 0.16));
  border: 1px solid rgba(0, 229, 255, 0.4);
  color: #d8f6ff;
  border-bottom-right-radius: 2px;
}
.msg-row.assistant .msg-bubble {
  background: rgba(10, 30, 66, 0.75);
  border: 1px solid rgba(0, 229, 255, 0.18);
  color: var(--text-secondary);
  border-bottom-left-radius: 2px;
  backdrop-filter: blur(6px);
}

/* 打字光标 */
.type-cursor {
  display: inline-block;
  width: 2px;
  height: 13px;
  margin-left: 2px;
  vertical-align: -2px;
  background: var(--color-primary);
  box-shadow: 0 0 6px rgba(0, 229, 255, 0.8);
  animation: cursor-blink 0.85s steps(1) infinite;
}
@keyframes cursor-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* 等待动画 */
.dot-flash { color: var(--color-primary); font-size: 11px; letter-spacing: 3px; animation: pulse-glow 1s infinite; }

/* ===== 快捷问题 ===== */
.ai-quick {
  flex-shrink: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 8px 12px 4px;
}
.quick-btn {
  padding: 4px 10px;
  font-size: 10.5px;
  color: #8fd8ff;
  background: rgba(0, 229, 255, 0.07);
  border: 1px solid rgba(0, 229, 255, 0.3);
  border-radius: 12px;
  cursor: pointer;
  transition: all var(--dur-fast);
  white-space: nowrap;
}
.quick-btn:hover:not(:disabled) {
  background: rgba(0, 229, 255, 0.18);
  color: #fff;
}
.quick-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* ===== 输入区 ===== */
.ai-input-row {
  flex-shrink: 0;
  display: flex;
  gap: 8px;
  padding: 8px 12px 12px;
}
.ai-input {
  flex: 1;
  min-width: 0;
  padding: 9px 12px;
  font-size: 12.5px;
  color: var(--text-primary);
  background: rgba(2, 10, 26, 0.7);
  border: 1px solid rgba(0, 229, 255, 0.3);
  border-radius: 5px;
  outline: none;
  transition: border-color var(--dur-fast), box-shadow var(--dur-fast);
}
.ai-input::placeholder { color: var(--text-faint); }
.ai-input:focus { border-color: rgba(0, 229, 255, 0.7); box-shadow: 0 0 10px rgba(0, 229, 255, 0.2); }
.ai-input:disabled { opacity: 0.55; }

.ai-send {
  width: 40px;
  border-radius: 5px;
  border: 1px solid rgba(0, 229, 255, 0.45);
  background: linear-gradient(135deg, rgba(0, 229, 255, 0.25), rgba(123, 104, 238, 0.25));
  color: #aef4ff;
  font-size: 15px;
  cursor: pointer;
  transition: all var(--dur-fast);
  display: flex;
  align-items: center;
  justify-content: center;
}
.ai-send:hover:not(:disabled) { box-shadow: 0 0 14px rgba(0, 229, 255, 0.4); color: #fff; }
.ai-send:disabled { opacity: 0.45; cursor: not-allowed; }

/* 发送 loading 圈 */
.send-spin {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(0, 229, 255, 0.3);
  border-top-color: #00e5ff;
  border-radius: 50%;
  animation: spin-rotate 0.8s linear infinite;
}
@keyframes spin-rotate {
  to { transform: rotate(360deg); }
}
</style>
