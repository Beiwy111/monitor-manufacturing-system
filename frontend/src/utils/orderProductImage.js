import img156 from '@picture/LCD面板图片.jpg'
import img238 from '@picture/整机组装.jpg'
import img27 from '@picture/外壳装配.jpg'
import imgOled from '@picture/LCD.png'
import imgDefault from '@picture/显示屏加工.jpg'

/** 按产品型号 / 面板类型匹配展示图 */
export function getOrderProductImage(order) {
  const text = `${order?.productModel || ''} ${order?.panelType || ''} ${order?.specification || ''}`.toLowerCase()
  if (text.includes('15.6') || text.includes('prd-001') || text.includes('dm-24')) return img156
  if (text.includes('23.8') || text.includes('电竞') || text.includes('dm-27') || text.includes('144hz')) return img238
  if (text.includes('27') || text.includes('4k') || text.includes('dm-32') || text.includes('hdr')) return img27
  if (text.includes('oled')) return imgOled
  return imgDefault
}
