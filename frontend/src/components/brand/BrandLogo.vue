<template>
  <component :is="tag" class="brand-logo" :class="[`brand-logo--${variant}`, { 'brand-logo--stack': stack }]">
    <img :src="BRAND_LOGO" :alt="BRAND_NAME" class="brand-logo__img" :style="imgStyle" />
    <span v-if="showText" class="brand-logo__text">
      <strong v-if="showName">{{ text || BRAND_NAME }}</strong>
      <small v-if="subtitle" class="brand-logo__sub">{{ subtitle }}</small>
    </span>
  </component>
</template>

<script setup>
import { computed } from 'vue'
import { BRAND_LOGO, BRAND_NAME } from '@/constants/brand'

const props = defineProps({
  tag: { type: String, default: 'div' },
  size: { type: Number, default: 32 },
  showText: { type: Boolean, default: true },
  showName: { type: Boolean, default: true },
  text: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  stack: { type: Boolean, default: false },
  variant: { type: String, default: 'default' }
})

const imgStyle = computed(() => ({
  width: `${props.size}px`,
  height: 'auto',
  maxHeight: `${props.size}px`
}))
</script>

<style scoped>
.brand-logo {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.brand-logo--stack {
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.brand-logo__img {
  object-fit: contain;
  flex-shrink: 0;
  display: block;
  background: transparent;
}

.brand-logo__text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.brand-logo__text strong {
  font-size: 14px;
  font-weight: 700;
  line-height: 1.2;
  color: inherit;
}

.brand-logo__sub {
  font-size: 11px;
  font-weight: 400;
  line-height: 1.3;
  opacity: 0.75;
}

.brand-logo--light .brand-logo__text strong {
  font-size: var(--fs-nav-brand, 16px);
  font-weight: var(--brand-weight, 700);
  color: #fff;
  letter-spacing: -0.2px;
}

.brand-logo--light .brand-logo__img {
  filter: brightness(2.1) contrast(1.05) saturate(1.35)
    drop-shadow(0 0 10px rgba(147, 211, 255, 0.55))
    drop-shadow(0 1px 3px rgba(255, 255, 255, 0.35));
}

.brand-logo--dark .brand-logo__text strong {
  color: #1f2329;
}

.brand-logo--sidebar .brand-logo__text strong {
  font-size: 15px;
  font-weight: 700;
  color: var(--sidebar-text-hover, #25272a);
  letter-spacing: 0.02em;
}
</style>
