import img1 from '@picture/显示器图片1.png'
import img2 from '@picture/显示器图片2.png'
import img3 from '@picture/显示器图片3.png'
import img4 from '@picture/显示器图片4.png'
import img5 from '@picture/显示器图片5.jpg'
import img6 from '@picture/显示器图片6.jpg'
import img7 from '@picture/显示器图片7.jpg'
import img8 from '@picture/显示器图片8.jpg'

/** 客户门户首页展示图（按 sort_order / PRD-001~008 对应 1~8） */
export const CUSTOMER_PRODUCT_IMAGES = [img1, img2, img3, img4, img5, img6, img7, img8]

const PLACEHOLDER = '/materials/products/placeholder.svg'

function productImageIndex(product) {
  if (!product) return -1
  const sort = Number(product.sortOrder)
  if (sort >= 1 && sort <= 8) return sort - 1
  const code = String(product.materialCode || '')
  const m = code.match(/PRD-00(\d)/)
  if (m) {
    const n = Number(m[1])
    if (n >= 1 && n <= 8) return n - 1
  }
  return -1
}

/** 客户门户产品图（本地 8 张显示器图） */
export function resolveCustomerProductImage(product) {
  const idx = productImageIndex(product)
  if (idx >= 0) return CUSTOMER_PRODUCT_IMAGES[idx]
  return resolveProductImage(product)
}

/** 解析产品展示图（详情页等：优先本地图，其次数据库 imageUrl） */
export function resolveProductImage(product) {
  const idx = productImageIndex(product)
  if (idx >= 0) return CUSTOMER_PRODUCT_IMAGES[idx]
  if (!product) return PLACEHOLDER
  if (product.imageUrl) return product.imageUrl
  if (product.materialCode) return `/materials/products/${String(product.materialCode).toLowerCase()}.jpg`
  return PLACEHOLDER
}

export function productImagePlaceholder() {
  return PLACEHOLDER
}

/** 从规格/名称提取尺寸与分辨率展示 */
export function parseProductDisplayMeta(product) {
  if (!product) return { size: '—', resolution: '—' }
  const text = `${product.materialName || ''} ${product.specification || ''}`
  const sizeMatch = text.match(/(\d+(?:\.\d+)?)\s*寸/)
  const resMatch = text.match(/(\d{3,4}\s*[x×]\s*\d{3,4})/i)
  return {
    size: sizeMatch ? `${sizeMatch[1]} 英寸` : '—',
    resolution: resMatch ? resMatch[1].replace(/\s/g, '') : (product.specification || '—')
  }
}

export function formatProductPrice(val) {
  if (val == null || val === '') return '—'
  return Number(val).toFixed(2)
}
