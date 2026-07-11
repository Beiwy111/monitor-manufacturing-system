/** 物料/成品图片：仓库页用本地图，采购页用编码映射兜底 */
import imgLcdPanel from '@picture/LCD面板图片.jpg'
import imgBacklight from '@picture/背光模组.png'
import imgDriverIc from '@picture/驱动IC.jpg'
import imgFrame from '@picture/钛合金边框.jpg'
import imgPcb from '@picture/PCB主板.jpg'
import imgPower from '@picture/电源适配器.jpg'

const IMG = (id) => `https://images.unsplash.com/${id}?auto=format&fit=crop&w=160&h=160`
const unsplash = (id) => `https://images.unsplash.com/photo-${id}?w=96&h=96&fit=crop&q=80`

const FINISHED_IMAGES = {
  'PRD-001': IMG('photo-1527443224154-c4a3942d3acf'),
  'PRD-002': IMG('photo-1625842268584-8f3296236761'),
  'PRD-003': IMG('photo-1563206767-5b18f218e8de')
}

const RAW_IMAGES = {
  'MAT-001': imgLcdPanel,
  'MAT-P01': imgLcdPanel,
  'MAT-P02': imgLcdPanel,
  'MAT-P03': imgLcdPanel,
  'MAT-002': imgBacklight,
  'MAT-B01': imgBacklight,
  'MAT-B02': imgBacklight,
  'MAT-B03': imgBacklight,
  'MAT-003': imgDriverIc,
  'MAT-M03': imgDriverIc,
  'MAT-004': imgFrame,
  'MAT-S01': imgFrame,
  'MAT-S03': imgFrame,
  'MAT-005': imgPcb,
  'MAT-M01': imgPcb,
  'MAT-M02': imgPcb,
  'MAT-M04': imgPcb,
  'MAT-006': imgPower,
  'MAT-S02': imgPower
}

/** 采购模块使用的 public 静态图映射 */
export const MATERIAL_IMAGE_MAP = {
  'MAT-001': '/materials/lcd.png',
  'MAT-002': '/materials/backlight.png',
  'MAT-003': '/materials/driver-ic.png',
  'MAT-004': '/materials/frame.png',
  'MAT-005': unsplash('1518770660439-4636190af475'),
  'MAT-006': unsplash('1583863788434-e58a36330cf0')
}

const FINISHED_FALLBACK = FINISHED_IMAGES['PRD-001']
const RAW_FALLBACK = imgLcdPanel

export const MATERIAL_PLACEHOLDER =
  'data:image/svg+xml,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="160" height="160" viewBox="0 0 160 160">' +
      '<rect width="160" height="160" fill="#f0f2f5"/>' +
      '<rect x="40" y="48" width="80" height="56" rx="4" fill="#dcdfe6"/>' +
      '<circle cx="60" cy="68" r="6" fill="#c0c4cc"/>' +
      '<path d="M40 92 L65 72 L90 88 L120 64 L120 104 L40 104 Z" fill="#c0c4cc"/>' +
      '</svg>'
  )

/** 按物料编码取图（采购模块） */
export function materialImageByCode(code) {
  if (!code) return ''
  if (MATERIAL_IMAGE_MAP[code]) return MATERIAL_IMAGE_MAP[code]
  if (RAW_IMAGES[code]) return RAW_IMAGES[code]
  return ''
}

function isFinished(row) {
  const code = row.materialCode || ''
  return row.warehouseCategory === 'FINISHED' || row.materialType === 'FINISHED' || code.startsWith('PRD')
}

/** 按行数据取图（仓库模块） */
export function materialImageUrl(row = {}) {
  const code = row.materialCode || ''
  if (isFinished(row)) {
    if (FINISHED_IMAGES[code]) return FINISHED_IMAGES[code]
    const name = row.materialName || ''
    if (name.includes('23.8') || name.includes('电竞')) return FINISHED_IMAGES['PRD-002']
    if (name.includes('27') || name.includes('4K')) return FINISHED_IMAGES['PRD-003']
    return FINISHED_FALLBACK
  }

  if (RAW_IMAGES[code]) return RAW_IMAGES[code]
  const mapped = materialImageByCode(code)
  if (mapped) return mapped

  const name = row.materialName || ''
  if (name.includes('面板') || name.includes('LCD') || name.includes('OLED') || name.includes('IPS')) {
    return imgLcdPanel
  }
  if (name.includes('背光') || name.includes('LED')) {
    return imgBacklight
  }
  if (name.includes('驱动') || name.includes('芯片') || name.includes('TCON') || name.includes('IC')) {
    return imgDriverIc
  }
  if (name.includes('边框') || name.includes('铝合金') || name.includes('钛合金')) {
    return imgFrame
  }
  if (name.includes('主控') || name.includes('PCB') || name.includes('主板')) {
    return imgPcb
  }
  if (name.includes('电源') || name.includes('适配器')) {
    return imgPower
  }
  return RAW_FALLBACK
}
