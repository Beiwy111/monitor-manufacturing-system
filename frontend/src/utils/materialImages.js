/**
 * 物料编码 → 展示缩略图。
 * MAT-001~004 使用 public/materials 下的本地产品图；
 * MAT-005/006 暂用 Unsplash 直链（电路板 / 电源适配器），放入本地图后替换即可。
 * 后端若返回 materialImageUrl 等字段则优先使用，此处仅作兜底。
 */
const unsplash = (id) => `https://images.unsplash.com/photo-${id}?w=96&h=96&fit=crop&q=80`

export const MATERIAL_IMAGE_MAP = {
  'MAT-001': '/materials/lcd.png',        // 15.6寸LCD面板
  'MAT-002': '/materials/backlight.png',  // 背光模组
  'MAT-003': '/materials/driver-ic.png',  // 驱动IC
  'MAT-004': '/materials/frame.png',       // 铝合金边框
  'MAT-005': unsplash('1518770660439-4636190af475'), // PCB主板 — 电路板特写
  'MAT-006': unsplash('1583863788434-e58a36330cf0')  // 电源适配器
}

/** 按物料编码取图，未命中返回空串 */
export function materialImageByCode(code) {
  return code ? (MATERIAL_IMAGE_MAP[code] || '') : ''
}
