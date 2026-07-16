import imgDeadPixel from '@picture/坏点检测.png'
import imgGamut from '@picture/色域检测.png'
import imgLeakage from '@picture/漏光检测.png'
import imgUniformity from '@picture/亮度均匀性检测.png'
import imgFlicker from '@picture/屏闪检测.png'

/** 成品五步仿真检测工位（固定顺序） */
export const FP_STATIONS = [
  {
    id: 'deadPixel',
    order: 1,
    title: '坏点检测',
    subtitle: '纯色画面逐像素检查亮点/暗点',
    image: imgDeadPixel,
    itemCodes: ['FP-02'],
    itemNameKeys: ['坏点']
  },
  {
    id: 'gamut',
    order: 2,
    title: '色域检测',
    subtitle: 'sRGB / DCI-P3 覆盖率与色准 ΔE',
    image: imgGamut,
    itemCodes: ['FP-04', 'FP-05', 'FP-06'],
    itemNameKeys: ['色域', 'sRGB', 'DCI', '色准']
  },
  {
    id: 'leakage',
    order: 3,
    title: '漏光检测',
    subtitle: '暗室黑场四角漏光与光晕',
    image: imgLeakage,
    itemCodes: [],
    itemNameKeys: ['漏光', '均匀']
  },
  {
    id: 'uniformity',
    order: 4,
    title: '亮度均匀性检测',
    subtitle: '九点亮度采样与中心偏差',
    image: imgUniformity,
    itemCodes: ['FP-09'],
    itemNameKeys: ['均匀']
  },
  {
    id: 'flicker',
    order: 5,
    title: '屏闪检测',
    subtitle: 'PWM 频率与肉眼可见闪烁',
    image: imgFlicker,
    itemCodes: [],
    itemNameKeys: ['闪烁', '响应']
  }
]

export function buildUnitList(sampleQty, batchNo) {
  const n = Math.max(1, Number(sampleQty) || 1)
  const prefix = (batchNo || 'BATCH').replace(/\s/g, '').slice(-8)
  return Array.from({ length: n }, (_, i) => ({
    unitNo: i + 1,
    serialNo: `${prefix}-${String(i + 1).padStart(3, '0')}`,
    status: 'PASS'
  }))
}

export function storageKeyForMatrix(inspectionId) {
  return `qc5-matrix:${inspectionId}`
}

/** 从已保存的 UNIT-xxx 检测项恢复矩阵（刷新后回显） */
export function parseMatrixFromItems(items, stations, batchNo, sampleQty) {
  const unitItems = (items || []).filter((i) => /^UNIT-\d{3}$/i.test(String(i.itemCode || '')))
  if (!unitItems.length) return null

  const sorted = [...unitItems].sort((a, b) =>
    String(a.itemCode).localeCompare(String(b.itemCode), undefined, { numeric: true })
  )

  const prefix = (batchNo || 'BATCH').replace(/\s/g, '').slice(-8)
  const units = sorted.map((it, i) => {
    const no = parseInt(String(it.itemCode).replace(/UNIT-/i, ''), 10) || i + 1
    const snFromName = String(it.itemName || '').replace(/^抽检成品\s*/, '').trim()
    return {
      unitNo: no,
      serialNo: snFromName || `${prefix}-${String(no).padStart(3, '0')}`,
      status: it.result === 'FAILED' ? 'FAIL' : 'PASS'
    }
  })

  const records = {}
  stations.forEach((s) => { records[s.id] = [] })

  sorted.forEach((it, i) => {
    const bits = String(it.measuredValue || '').padEnd(stations.length, '1').slice(0, stations.length)
    const remark = String(it.remark || '')
    const parts = remark.split(' · ')
    stations.forEach((s, si) => {
      const passed = bits[si] !== '0'
      records[s.id][i] = {
        passed,
        saved: true,
        measuredValue: passed ? '合格' : '不合格',
        remark: passed ? '' : remark,
        defectType: passed ? '' : (parts[0] || ''),
        defectLocation: passed ? '' : (parts[1] || ''),
        defectLevel: it.defectLevel || (passed ? '' : 'GENERAL')
      }
    })
  })

  return { units, records }
}
