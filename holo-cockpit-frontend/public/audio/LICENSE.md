# 背景音乐版权说明（Audio License）

本项目使用的背景音乐均为 **公有领域（Public Domain / CC0）** 授权，可安全用于任何商业与非商业场景，无需付费、无需授权。

## 曲目清单

| 文件 | 曲目 | 作曲 | 原始时长 | 授权 |
|------|------|------|----------|------|
| `bgm-1-tech.mp3` | Arpent | Kevin MacLeod | 2:42 | CC0（公有领域） |
| `bgm-2-dark.mp3` | Beat One | Kevin MacLeod | 3:00 | CC0（公有领域） |
| `bgm-3-calm.mp3` | Meditating Beat | Kevin MacLeod | 2:37 | CC0（公有领域） |

## 来源

- 原始发布平台：FreePD.com（Kevin MacLeod 将其部分作品以 CC0 公有领域形式发布于该平台；该站已于 2025 年停止运营）
- 曲目描述（来自原发布页）：
  - **Arpent** — Upbeat, uplifting and inspiring. Technology and innovation.（明快、振奋、科技与创新感，作为大屏主背景音乐）
  - **Beat One** — Upbeat, drum and bass styled track. Minimalistic and cool. Modern and dark.（极简冷静的现代暗色电子，契合驾驶舱深色科技风）
  - **Meditating Beat** — Corporate to casual gaming.（企业级轻快节奏，适合长时间驻留场景）

## 授权条款要点（CC0 1.0 Universal）

在法律允许的最大范围内，作者已放弃本作品的所有版权及邻接权。任何人可以复制、修改、分发、表演本作品，包括商业用途，无需任何授权或署名。

> 注：本项目仍在代码注释与本文件中主动标注了曲目与作者信息（Kevin MacLeod），作为对创作者的尊重性署名，非授权义务。

## 技术说明

- 交互音效（按键音、成功音、失败音、预警音等）由 Web Audio API 实时程序化合成，无第三方素材版权
- 音乐播放由 `src/audio/manager.js`（音频管理器 v3）统一管理：播放列表循环连播、音量/静音/曲目偏好记忆（localStorage）、切曲事件同步播放器 UI
