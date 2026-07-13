<template>
  <section class="board-frame">
    <header class="board-frame__head">
      <div class="board-frame__tabs">
        <span
          v-for="tab in tabs"
          :key="tab"
          class="board-frame__tab"
          :class="{ 'board-frame__tab--active': tab === activeTab }"
          @click="$emit('tab-change', tab)"
        >{{ tab }}</span>
      </div>
      <span v-if="extra" class="board-frame__extra">{{ extra }}</span>
    </header>
    <div class="board-frame__body">
      <slot />
    </div>
  </section>
</template>

<script setup>
defineProps({
  tabs: { type: Array, default: () => [] },
  activeTab: { type: String, default: '' },
  extra: { type: String, default: '' }
})
defineEmits(['tab-change'])
</script>

<style scoped>
.board-frame {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #fff;
  border: 1px solid #e8eaf0;
  border-radius: 10px;
  box-shadow: 0 4px 20px rgba(31, 41, 55, 0.06);
}

.board-frame__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid #f0f2f5;
  flex-shrink: 0;
}

.board-frame__tabs {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.board-frame__tab {
  padding: 4px 10px;
  font-size: 11px;
  color: #64748b;
  cursor: pointer;
  border: 1px solid transparent;
  border-radius: 6px;
  transition: all 0.2s;
}

.board-frame__tab--active {
  color: #172033;
  border-color: #e8eaf0;
  background: #f3f6f9;
  font-weight: 600;
}

.board-frame__extra {
  font-size: 10px;
  color: #94a3b8;
}

.board-frame__body {
  flex: 1;
  min-height: 0;
  padding: 8px 10px;
  overflow: hidden;
}
</style>
