import { nextTick } from 'vue'

export function scrollElementToCenter(container, element, behavior = 'smooth') {
  if (!container || !element) return
  const containerRect = container.getBoundingClientRect()
  const elementRect = element.getBoundingClientRect()
  const elementTop = elementRect.top - containerRect.top + container.scrollTop
  const scrollTop = elementTop - (container.clientHeight - elementRect.height) / 2
  container.scrollTo({
    top: Math.max(0, scrollTop),
    behavior
  })
}

export function scrollChildIntoView(container, pickElement, behavior = 'smooth') {
  nextTick(() => {
    if (!container) return
    const element = typeof pickElement === 'function'
      ? pickElement(container)
      : container.querySelector(pickElement)
    if (element) scrollElementToCenter(container, element, behavior)
  })
}
