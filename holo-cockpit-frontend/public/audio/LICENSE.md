# 背景音乐版权说明（Audio License）

本项目背景音乐为用户提供的本地曲目集（共 8 首，免版权音乐平台下载合集）。

## 曲目清单

| 文件 | 曲目 | 艺术家 |
|------|------|--------|
| `ghost-cities.mp3` | Ghost Cities | BXRDVJA |
| `other-worlds.mp3` | Other Worlds | Downtown Binary |
| `fly-up-high.mp3` | Fly up High | Flint |
| `to-the-stars.mp3` | To the Stars | Hotham |
| `sultans-of-streams.mp3` | Sultans of Streams | Jozeque |
| `quantum-world.mp3` | Quantum World | Magiksolo |
| `dusk.mp3` | Dusk | nuer self |
| `themis.mp3` | Themis | Sergey Azbel |

> 注：曲目均为电子/氛围风格，适合数据可视化大屏场景，仅用于课程设计演示。

## 技术说明

- 交互音效（按键音、成功音、失败音、预警音等）由 Web Audio API 实时程序化合成，无第三方素材版权
- 音乐播放由 `src/audio/manager.js`（音频管理器 v3）统一管理：播放列表循环连播、音量/静音/曲目偏好记忆（localStorage）、切曲事件同步播放器 UI
- 曲目切换：播放器 UI 的 next/prev 按钮或自动连播
