<template>
  <header class="header-bar">
    <div class="header-bg"></div>

    <!-- 左右 HUD 翼形装饰 -->
    <div class="hud-wing wing-left">
      <div class="wing-line l1"></div>
      <div class="wing-line l2"></div>
      <div class="wing-tick" v-for="n in 8" :key="n" :style="{ '--n': n }"></div>
    </div>
    <div class="hud-wing wing-right">
      <div class="wing-line l1"></div>
      <div class="wing-line l2"></div>
      <div class="wing-tick" v-for="n in 8" :key="n" :style="{ '--n': n }"></div>
    </div>

    <div class="header-content">
      <!-- 左：系统时钟 -->
      <div class="header-side">
        <div class="header-stat">
          <span class="stat-glyph">◈</span>
          <div class="stat-col">
            <span class="stat-label">SYSTEM TIME</span>
            <span class="stat-value font-data">{{ currentTime }}</span>
          </div>
        </div>
        <div class="header-stat">
          <span class="stat-glyph ok">◉</span>
          <div class="stat-col">
            <span class="stat-label">DATA STREAM</span>
            <span class="stat-value ok">实时更新中</span>
          </div>
        </div>
      </div>

      <!-- 中：glitch 故障字标题 -->
      <div class="header-center">
        <h1 class="main-title title-glitch font-display" data-text="华为全息数据驾驶舱">华为全息数据驾驶舱</h1>
        <div class="sub-title">
          <span class="line"></span>
          <span class="diamond"></span>
          <span class="text">HUAWEI DATA COCKPIT · 业务数据全景</span>
          <span class="diamond"></span>
          <span class="line"></span>
        </div>
      </div>

      <!-- 右：音乐播放器 / 音量 / 管理后台 / AI -->
      <div class="header-side right">
        <div class="music-player" :title="trackTooltip">
          <button class="icon-btn mp-btn" title="上一曲" @click="onPrevTrack">⏮</button>
          <div class="mp-info">
            <span class="mp-note" :class="{ playing: !muted }">♫</span>
            <span class="mp-name">{{ trackName }}</span>
          </div>
          <button class="icon-btn mp-btn" title="下一曲" @click="onNextTrack">⏭</button>
        </div>
        <div class="volume-ctl">
          <button class="icon-btn" :title="muted ? '取消静音' : '静音'" @click="onToggleMute">{{ muted ? '🔇' : '🔊' }}</button>
          <input
            class="volume-slider"
            type="range"
            min="0"
            max="100"
            v-model.number="volume"
            @input="onVolume"
            :aria-label="'音量 ' + volume + '%'"
          />
        </div>
        <router-link to="/admin/dashboard" class="admin-entry">
          <span class="entry-glyph">⬡</span> 管理后台
        </router-link>
        <button class="ai-entry" title="AI 数据分析师" @click="$emit('open-ai')">
          <span class="ai-emoji">🤖</span> AI
        </button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { audioManager } from '@/audio/manager'

defineEmits(['open-ai'])

const currentTime = ref('')
const volume = ref(Math.round(audioManager.volume * 100))
const muted = ref(audioManager.muted)
let timer = null

/* ===== 音乐播放器 ===== */
const trackName = ref(audioManager.currentTrack?.name || '合成音源')
const trackTooltip = computed(() =>
  audioManager.useFile ? `当前曲目：${trackName.value}（点击 ⏮/⏭ 切换）` : '程序化合成音源（未找到本地音乐）'
)

function onAudioTrack(e) {
  if (e?.detail?.name) trackName.value = e.detail.name
}

function onPrevTrack() {
  audioManager.prev()
}

function onNextTrack() {
  audioManager.next()
}

function pad(n) { return n < 10 ? '0' + n : n }

function updateTime() {
  const d = new Date()
  currentTime.value = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function onVolume() {
  audioManager.setVolume(volume.value / 100)
}

function onToggleMute() {
  muted.value = audioManager.toggleMute()
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  // 初始化曲名 + 监听切曲事件（手动切歌 / 自动连播均会触发）
  const cur = audioManager.currentTrack
  if (cur?.name) trackName.value = cur.name
  window.addEventListener('hw-audio-track', onAudioTrack)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  window.removeEventListener('hw-audio-track', onAudioTrack)
})
</script>

<style scoped>
.header-bar {
  height: 88px;
  position: relative;
  margin-bottom: 10px;
  flex-shrink: 0;
}

.header-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(0, 165, 255, 0.16) 0%, transparent 100%);
  -webkit-mask-image: linear-gradient(180deg, #000 50%, transparent 100%);
          mask-image: linear-gradient(180deg, #000 50%, transparent 100%);
}
.header-bg::after {
  content: '';
  position: absolute;
  bottom: 0; left: 50%;
  transform: translateX(-50%);
  width: 46%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0, 229, 255, 0.8), transparent);
}

/* ===== HUD 翼形装饰 ===== */
.hud-wing {
  position: absolute;
  bottom: 10px;
  width: 280px;
  height: 40px;
  pointer-events: none;
  opacity: 0.9;
}
.wing-left { left: 0; }
.wing-right { right: 0; transform: scaleX(-1); }

.wing-line {
  position: absolute;
  bottom: 0;
  height: 1px;
  background: linear-gradient(90deg, rgba(0, 229, 255, 0.55), transparent);
}
.wing-left .l1 { width: 100%; }
.wing-left .l2 {
  width: 62%;
  bottom: 9px;
  background: linear-gradient(90deg, rgba(123, 104, 238, 0.45), transparent);
}

.wing-tick {
  position: absolute;
  bottom: 0;
  left: calc(var(--n) * 12.5% - 4px);
  width: 1px;
  background: rgba(0, 229, 255, 0.5);
  animation: tick-breathe 3s ease-in-out infinite;
  animation-delay: calc(var(--n) * -0.4s);
}
@keyframes tick-breathe {
  0%, 100% { height: 4px; opacity: 0.4; }
  50% { height: 9px; opacity: 1; }
}

/* ===== 内容 ===== */
.header-content {
  height: 100%;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  padding: 0 26px;
  position: relative;
  z-index: 1;
}

.header-side {
  display: flex;
  align-items: center;
  gap: 10px;
}
.header-side.right { justify-content: flex-end; }

.header-stat {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 14px;
  background: rgba(5, 18, 42, 0.65);
  border: 1px solid rgba(0, 229, 255, 0.2);
  border-radius: 3px;
  backdrop-filter: blur(6px);
  position: relative;
}
.header-stat::before {
  content: '';
  position: absolute;
  left: 0; top: 20%; bottom: 20%;
  width: 2px;
  background: var(--color-primary);
  box-shadow: var(--glow-cyan);
}
.stat-glyph {
  font-size: 15px;
  color: var(--color-primary);
  animation: pulse-glow 2.5s infinite;
}
.stat-glyph.ok { color: var(--color-success); }
.stat-col { display: flex; flex-direction: column; gap: 1px; }
.stat-label {
  font-family: var(--font-data);
  font-size: 8px;
  letter-spacing: 2px;
  color: var(--text-faint);
  font-weight: 600;
}
.stat-value {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 600;
  letter-spacing: 1px;
}
.stat-value.ok { color: var(--color-success); }

/* ===== 音乐播放器 ===== */
.music-player {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  background: rgba(5, 18, 42, 0.65);
  border: 1px solid rgba(0, 229, 255, 0.2);
  border-radius: 3px;
  backdrop-filter: blur(6px);
}
.mp-btn { font-size: 11px; opacity: 0.75; }
.mp-btn:hover { opacity: 1; }
.mp-info {
  display: flex;
  align-items: center;
  gap: 6px;
  max-width: 150px;
  overflow: hidden;
}
.mp-note {
  font-size: 12px;
  color: var(--text-faint);
  transition: color var(--dur-fast);
}
.mp-note.playing {
  color: var(--color-primary);
  animation: note-swing 1.2s ease-in-out infinite;
}
@keyframes note-swing {
  0%, 100% { transform: rotate(-10deg) scale(1); }
  50%      { transform: rotate(12deg) scale(1.15); }
}
.mp-name {
  font-size: 11px;
  letter-spacing: 0.5px;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ===== 音量控制 ===== */
.volume-ctl {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: rgba(5, 18, 42, 0.65);
  border: 1px solid rgba(0, 229, 255, 0.2);
  border-radius: 3px;
  backdrop-filter: blur(6px);
}
.icon-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  padding: 2px;
  opacity: 0.9;
  transition: transform var(--dur-fast), opacity var(--dur-fast);
}
.icon-btn:hover { transform: scale(1.15); opacity: 1; }

.volume-slider {
  -webkit-appearance: none;
  appearance: none;
  width: 72px;
  height: 3px;
  border-radius: 2px;
  background: linear-gradient(90deg, var(--color-primary), var(--color-accent));
  outline: none;
  cursor: pointer;
}
.volume-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 11px;
  height: 11px;
  border-radius: 50%;
  background: #dff8ff;
  border: 2px solid var(--color-primary);
  box-shadow: 0 0 8px rgba(0, 229, 255, 0.7);
  transition: transform var(--dur-fast);
}
.volume-slider::-webkit-slider-thumb:hover { transform: scale(1.25); }
.volume-slider::-moz-range-thumb {
  width: 11px;
  height: 11px;
  border-radius: 50%;
  background: #dff8ff;
  border: 2px solid var(--color-primary);
  box-shadow: 0 0 8px rgba(0, 229, 255, 0.7);
}

/* ===== 管理后台 / AI 按钮 ===== */
.admin-entry {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 8px 14px;
  background: linear-gradient(135deg, rgba(0, 229, 255, 0.14), rgba(123, 104, 238, 0.14));
  border: 1px solid rgba(0, 229, 255, 0.35);
  border-radius: 3px;
  font-size: 13px;
  color: #7ee7ff;
  text-decoration: none;
  transition: all var(--dur-fast);
  white-space: nowrap;
  position: relative;
  overflow: hidden;
}
.admin-entry::after {
  content: '';
  position: absolute;
  top: 0; left: 0;
  width: 30%; height: 100%;
  background: linear-gradient(100deg, transparent, rgba(255, 255, 255, 0.12), transparent);
  transform: translateX(-150%) skewX(-15deg);
}
.admin-entry:hover {
  background: linear-gradient(135deg, rgba(0, 229, 255, 0.28), rgba(123, 104, 238, 0.28));
  box-shadow: 0 0 18px rgba(0, 229, 255, 0.35);
  color: #fff;
  border-color: rgba(0, 229, 255, 0.6);
}
.admin-entry:hover::after { animation: title-sweep 0.8s ease; }
.entry-glyph { font-size: 14px; }

.ai-entry {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: linear-gradient(135deg, rgba(0, 255, 163, 0.12), rgba(0, 229, 255, 0.16));
  border: 1px solid rgba(0, 255, 163, 0.4);
  border-radius: 3px;
  font-size: 13px;
  font-weight: 600;
  color: #7dffd0;
  cursor: pointer;
  transition: all var(--dur-fast);
  white-space: nowrap;
}
.ai-emoji { font-size: 14px; }
.ai-entry:hover {
  background: linear-gradient(135deg, rgba(0, 255, 163, 0.26), rgba(0, 229, 255, 0.3));
  box-shadow: 0 0 18px rgba(0, 255, 163, 0.3);
  color: #fff;
}

/* ===== Glitch 故障字标题 ===== */
.header-center {
  text-align: center;
  padding: 0 30px;
}
.main-title {
  position: relative;
  font-size: 38px;
  font-weight: 800;
  letter-spacing: 9px;
  line-height: 1.15;
  margin-bottom: 6px;
  padding: 0 10px;
  text-indent: 10px;
  animation: fade-in 0.9s var(--ease-out) both;
  background: linear-gradient(180deg, #ffffff 0%, #9beaff 55%, #00a5ff 100%);
  -webkit-background-clip: text;
          background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 0 14px rgba(0, 229, 255, 0.45));
}
/* 红青双色残影，每 8s 触发一次错位 */
.title-glitch::before,
.title-glitch::after {
  content: attr(data-text);
  position: absolute;
  inset: 0;
  padding: 0 10px;
  text-indent: 10px;
  background: none;
  -webkit-text-fill-color: currentColor;
  opacity: 0;
  pointer-events: none;
}
.title-glitch::before {
  color: #ff4d6a;
  animation: glitch-red 8s steps(1) infinite;
}
.title-glitch::after {
  color: #00e5ff;
  animation: glitch-cyan 8s steps(1) infinite;
}
@keyframes glitch-red {
  0%, 2.8%, 3.7%, 49.8%, 51%, 100% { opacity: 0; transform: translate(0, 0); }
  3% { opacity: 0.8; transform: translate(-4px, 2px); }
  3.4% { opacity: 0.8; transform: translate(3px, -1px); }
  50% { opacity: 0.65; transform: translate(-2px, 1px); }
  50.4% { opacity: 0.65; transform: translate(4px, -2px); }
}
@keyframes glitch-cyan {
  0%, 2.8%, 3.7%, 49.8%, 51%, 100% { opacity: 0; transform: translate(0, 0); }
  3.1% { opacity: 0.8; transform: translate(4px, -2px); }
  3.5% { opacity: 0.8; transform: translate(-3px, 2px); }
  50.1% { opacity: 0.65; transform: translate(3px, 1px); }
  50.5% { opacity: 0.65; transform: translate(-4px, -1px); }
}

.sub-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--text-muted);
  font-size: 11.5px;
  letter-spacing: 3.5px;
  animation: fade-in 1s var(--ease-out) 0.25s both;
}
.sub-title .line {
  width: 70px;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--color-primary), transparent);
}
.sub-title .diamond {
  width: 5px;
  height: 5px;
  background: var(--color-primary);
  transform: rotate(45deg);
  box-shadow: 0 0 6px rgba(0, 229, 255, 0.6);
  animation: pulse-glow 3s infinite;
}

@media (max-width: 1600px) {
  .hud-wing { width: 200px; }
}
@media (max-width: 1366px) {
  .header-bar { height: 74px; }
  .main-title { font-size: 30px; letter-spacing: 7px; }
  .sub-title { font-size: 10px; }
  .sub-title .line { width: 44px; }
  .hud-wing { width: 150px; }
  .header-content { padding: 0 16px; }
  .volume-slider { width: 52px; }
}
</style>
