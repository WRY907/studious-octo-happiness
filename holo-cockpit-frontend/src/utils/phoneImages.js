/* ============================================
   机型图片库（华为商城公开页合规爬取，本地化于 public/images/phones/）
   覆盖：当前在售 lineup + 上一代主力机型，共 19 款
   匹配规则：机型名小写化后按 match 关键词包含匹配，
   库顺序保证"更具体的型号在前"（如 mate 80 pro max 先于 mate 80 pro）
   ============================================ */

const PHONE_LIBRARY = [
  { key: 'mate-xt2', match: ['mate xt 2', 'mate xt'] },
  { key: 'mate-x7', match: ['mate x7'] },
  { key: 'pura-x-max', match: ['pura x max'] },
  { key: 'mate-x6', match: ['mate x6'] },
  { key: 'mate-80-rs', match: ['mate 80 rs'] },
  { key: 'mate-80-pro-max', match: ['mate 80 pro max'] },
  { key: 'mate-80-pro-plus', match: ['mate 80 pro+'] },
  { key: 'pura-x-view', match: ['pura x view'] },
  { key: 'mate-80-pro', match: ['mate 80 pro'] },
  { key: 'pura-90-pro-max', match: ['pura 90 pro max'] },
  { key: 'pura-90-pro', match: ['pura 90 pro'] },
  { key: 'mate-70-pro', match: ['mate 70 pro'] },
  { key: 'pura-80-pro', match: ['pura 80 pro'] },
  { key: 'nova-16-se', match: ['nova 16 se'] },
  { key: 'nova-16', match: ['nova 16'] },
  { key: 'nova-14-pro', match: ['nova 14 pro'] },
  { key: 'nova-14', match: ['nova 14'] },
  { key: 'mate-80', match: ['mate 80'] },
  { key: 'pura-80', match: ['pura 80'] },
  { key: 'changxiang-90-pro-max', match: ['畅享 90', '畅享90'] },
  { key: 'pocket-3', match: ['pocket 3', 'pocket'] },
  { key: 'changxiang-80x', match: ['畅享 80x', '畅享80x'] },
  { key: 'changxiang-80', match: ['畅享 80', '畅享80'] }
]

/**
 * 机型名 → 本地图片路径
 * @param {string} modelName 机型名（如 "HUAWEI Mate 80 Pro Max"）
 * @returns {string|null} 命中返回 "/images/phones/xxx.png"，未命中返回 null
 */
export function phoneImg(modelName) {
  const n = String(modelName || '').toLowerCase()
  for (const p of PHONE_LIBRARY) {
    if (p.match.some(m => n.includes(m))) return '/images/phones/' + p.key + (p.key === 'pocket-3' ? '.jpg' : '.png')
  }
  return null
}

export { PHONE_LIBRARY }
