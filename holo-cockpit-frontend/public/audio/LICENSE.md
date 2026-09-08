# 背景音乐版权说明（Audio License）

本项目背景音乐来自 **Incompetech（Kevin MacLeod）**，采用 **CC-BY 4.0** 授权，可自由用于商业与非商业场景，需保留作者署名。

## 曲目清单

| 文件 | 曲目 | 作曲 | 授权 |
|------|------|------|------|
| `bgm-future-gladiator.mp3` | Future Gladiator | Kevin MacLeod | CC-BY 4.0 |
| `bgm-digital-lemonade.mp3` | Digital Lemonade | Kevin MacLeod | CC-BY 4.0 |
| `bgm-inspired.mp3` | Inspired | Kevin MacLeod | CC-BY 4.0 |

## 来源与风格

- 来源：https://incompetech.com/music/royalty-free/ （Kevin MacLeod 免版税音乐库）
- 风格定位（科技企业风 · 成就展示 · 数据可视化）：
  - **Future Gladiator** — 大气磅礴的成就史诗感，适合企业成果展示主背景
  - **Digital Lemonade** — 电子数据流节奏，契合数据可视化动效
  - **Inspired** — 上扬励志的科技氛围，适合汇报演示场景

## 授权条款要点（CC-BY 4.0）

可复制、修改、分发、商业使用；需署名作者（Kevin MacLeod / incompetech.com）。
完整条款：https://creativecommons.org/licenses/by/4.0/

## 技术说明

- 交互音效（按键音、成功音、失败音、预警音等）由 Web Audio API 实时程序化合成，无第三方素材版权
- 音乐播放由 `src/audio/manager.js`（音频管理器 v3）统一管理：播放列表循环连播、音量/静音/曲目偏好记忆（localStorage）、切曲事件同步播放器 UI
- 曲目切换：播放器 UI 的 next/prev 按钮或自动连播
