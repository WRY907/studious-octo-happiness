/* ============================================
   音频管理器 v2.1
   - 背景音乐：优先播放 public/ 下的本地音乐文件（bgm.mp3 / bgm.m4a / bgm.ogg /
     bgm.wav / bgm.flac 按序探测，取第一个存在的，循环播放）
   - 无本地文件时回退：Web Audio 程序化合成 BGM（双失谐锯齿 Pad + 五声音阶琶音）
   - 交互音：click / feedback / alert（始终用合成短音）
   - ducking：交互音播放时 BGM 自动避让
   ============================================ */

/* 候选本地音乐文件（public/ 下），按顺序探测第一个存在的 */
const BGM_CANDIDATES = ['/bgm.mp3', '/bgm.m4a', '/bgm.ogg', '/bgm.wav', '/bgm.flac']

class AudioManager {
  constructor() {
    this.ctx = null
    this.master = null   // 合成总线（交互音 + 合成BGM）
    this.bgm = null      // 合成 BGM 总线（可被 ducking）
    this.bgMusic = null  // 本地音乐 <audio> 元素（文件模式）
    this.useFile = false // 是否使用本地音乐文件模式
    this.volume = 0.6    // 总音量 0~1
    this.muted = false
    this._bgmBase = 1    // 合成 BGM 基准增益
    this._arpTimer = null
    this._duckTimer = null
    this._initHandler = null
    this._visHandler = null
  }

  /* 入口：绑定一次性 click，在用户首次交互时真正初始化（浏览器自动播放策略） */
  init() {
    if (this._initHandler) return
    this._initHandler = () => this._setup()
    document.addEventListener('click', this._initHandler, { once: true, capture: true })
  }

  async _setup() {
    try {
      if (this.ctx) { this._resume(); return }

      // 先探测本地音乐文件（public/ 下多格式按序探测，取第一个存在的）
      let bgmUrl = null
      for (const url of BGM_CANDIDATES) {
        if (await this._probeFile(url)) { bgmUrl = url; break }
      }

      const AC = window.AudioContext || window.webkitAudioContext
      if (AC) {
        this.ctx = new AC()
        this.master = this.ctx.createGain()
        this.master.gain.value = this.muted ? 0 : this.volume
        this.master.connect(this.ctx.destination)
        this.bgm = this.ctx.createGain()
        this.bgm.gain.value = this._bgmBase
        this.bgm.connect(this.master)
      }

      if (bgmUrl) {
        // ===== 文件模式：本地音乐循环播放 =====
        this.useFile = true
        this.bgMusic = new Audio(bgmUrl)
        this.bgMusic.loop = true
        this.bgMusic.volume = this._bgmFileVolume()
        this.bgMusic.play().catch(() => { /* 播放失败回退合成 */ })
        this.bgMusic.addEventListener('error', () => {
          // 文件播放失败（损坏/格式不支持）→ 回退合成 BGM
          this.useFile = false
          this.bgMusic = null
          this._startPad()
          this._startArp()
        })
      } else if (this.ctx) {
        // ===== 合成模式 =====
        this._startPad()
        this._startArp()
      }

      this._visHandler = () => {
        try {
          if (document.hidden) {
            if (this.ctx) this.ctx.suspend()
            if (this.bgMusic) this.bgMusic.pause()
          } else {
            if (this.ctx) this.ctx.resume()
            if (this.bgMusic && !this.muted) this.bgMusic.play().catch(() => {})
          }
        } catch (e) { /* 静默 */ }
      }
      document.addEventListener('visibilitychange', this._visHandler)
    } catch (e) { /* 静默 */ }
  }

  /* 探测本地音乐文件是否存在（HEAD 请求，404 即无） */
  _probeFile(url) {
    return fetch(url, { method: 'HEAD' })
      .then(r => r.ok)
      .catch(() => false)
  }

  /* 文件模式基准音量（略低于交互音） */
  _bgmFileVolume() {
    return this.muted ? 0 : Math.min(1, this.volume * 0.8)
  }

  _resume() {
    try {
      if (this.ctx && this.ctx.state === 'suspended') this.ctx.resume()
      if (this.bgMusic && !this.muted && this.bgMusic.paused) this.bgMusic.play().catch(() => {})
    } catch (e) { /* 静默 */ }
  }

  /* ===== 合成 BGM 第一层：双失谐锯齿 Pad + 低通 + 慢 LFO，音量 0.04 ===== */
  _startPad() {
    if (!this.ctx) return
    const ctx = this.ctx
    const padGain = ctx.createGain()
    padGain.gain.value = 0.04
    const filter = ctx.createBiquadFilter()
    filter.type = 'lowpass'
    filter.frequency.value = 520
    filter.Q.value = 0.7
    // 两支失谐锯齿波
    ;[0, 1].forEach(i => {
      const osc = ctx.createOscillator()
      osc.type = 'sawtooth'
      osc.frequency.value = 110 // A2
      osc.detune.value = i === 0 ? -7 : 9
      osc.connect(filter)
      osc.start()
    })
    filter.connect(padGain)
    padGain.connect(this.bgm)
    // 慢 LFO 调制滤波频率，产生缓慢呼吸感
    const lfo = ctx.createOscillator()
    lfo.frequency.value = 0.08
    const lfoGain = ctx.createGain()
    lfoGain.gain.value = 200
    lfo.connect(lfoGain)
    lfoGain.connect(filter.frequency)
    lfo.start()
  }

  /* ===== 合成 BGM 第二层：五声音阶（A C D E G）随机琶音，每 0.5s 短音 ===== */
  _startArp() {
    if (!this.ctx) return
    const scale = [220, 261.63, 293.66, 329.63, 392] // A3 C4 D4 E4 G4
    this._arpTimer = setInterval(() => {
      try {
        if (document.hidden || !this.ctx || this.ctx.state !== 'running') return
        this._note(scale[Math.floor(Math.random() * scale.length)], 'triangle', 0.05, 0.9, 0, this.bgm)
      } catch (e) { /* 静默 */ }
    }, 500)
  }

  /* ===== 通用短音 ===== */
  _note(freq, type, vol, decay, delay = 0, dest = null) {
    try {
      if (!this.ctx || this.ctx.state !== 'running') return
      const t = this.ctx.currentTime + delay
      const osc = this.ctx.createOscillator()
      const g = this.ctx.createGain()
      osc.type = type
      osc.frequency.value = freq
      g.gain.setValueAtTime(vol, t)
      g.gain.exponentialRampToValueAtTime(0.0001, t + decay)
      osc.connect(g)
      g.connect(dest || this.master)
      osc.start(t)
      osc.stop(t + decay + 0.05)
    } catch (e) { /* 静默 */ }
  }

  /* ===== ducking：交互音播放时 BGM 渐降 0.35 倍，800ms 后渐回 ===== */
  _duck() {
    try {
      clearTimeout(this._duckTimer)
      if (this.useFile && this.bgMusic) {
        // 文件模式：直接调 audio 音量（无精确 ramp，用步进近似）
        const base = this._bgmFileVolume()
        this.bgMusic.volume = Math.max(0, base * 0.35)
        this._duckTimer = setTimeout(() => {
          try { if (this.bgMusic) this.bgMusic.volume = this._bgmFileVolume() } catch (e) { /* 静默 */ }
        }, 800)
      } else if (this.ctx && this.bgm) {
        this.bgm.gain.setTargetAtTime(this._bgmBase * 0.35, this.ctx.currentTime, 0.05)
        this._duckTimer = setTimeout(() => {
          try {
            if (this.ctx && this.bgm) this.bgm.gain.setTargetAtTime(this._bgmBase, this.ctx.currentTime, 0.12)
          } catch (e) { /* 静默 */ }
        }, 800)
      }
    } catch (e) { /* 静默 */ }
  }

  /* ===== 按键音：880Hz square 30ms ===== */
  click() {
    this._note(880, 'square', 0.1, 0.03)
    this._duck()
  }

  /* ===== 反馈音：两音上行 660 → 990，各 80ms ===== */
  feedback() {
    this._note(660, 'sine', 0.12, 0.08)
    this._note(990, 'sine', 0.12, 0.08, 0.1)
    this._duck()
  }

  /* ===== 预警音：440/554 三连音 ===== */
  alert() {
    this._note(440, 'triangle', 0.14, 0.14)
    this._note(554, 'triangle', 0.14, 0.14, 0.16)
    this._note(440, 'triangle', 0.14, 0.14, 0.32)
  }

  /* ===== 总音量 0~1 ===== */
  setVolume(v) {
    this.volume = Math.min(1, Math.max(0, Number(v) || 0))
    try {
      if (this.ctx && this.master) {
        this.master.gain.setTargetAtTime(this.muted ? 0 : this.volume, this.ctx.currentTime, 0.03)
      }
      if (this.bgMusic) this.bgMusic.volume = this._bgmFileVolume()
    } catch (e) { /* 静默 */ }
  }

  /* ===== 静音切换，返回当前静音状态 ===== */
  toggleMute() {
    this.muted = !this.muted
    try {
      if (this.ctx && this.master) {
        this.master.gain.setTargetAtTime(this.muted ? 0 : this.volume, this.ctx.currentTime, 0.03)
      }
      if (this.bgMusic) {
        this.bgMusic.volume = this._bgmFileVolume()
        if (this.muted) this.bgMusic.pause()
        else this.bgMusic.play().catch(() => {})
      }
    } catch (e) { /* 静默 */ }
    return this.muted
  }

  /* ===== 当前 BGM 模式（供 UI 显示音源） ===== */
  get sourceLabel() {
    return this.useFile ? '本地音乐' : '合成音源'
  }
}

/* 导出单例 */
export const audioManager = new AudioManager()
export default audioManager
