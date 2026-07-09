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
    <span class="board-frame__corner board-frame__corner--tl" />
    <span class="board-frame__corner board-frame__corner--tr" />
    <span class="board-frame__corner board-frame__corner--bl" />
    <span class="board-frame__corner board-frame__corner--br" />
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
  background: linear-gradient(180deg, rgba(8, 28, 58, 0.92) 0%, rgba(5, 18, 40, 0.96) 100%);
  border: 1px solid rgba(0, 180, 255, 0.25);
  box-shadow: inset 0 0 24px rgba(0, 120, 255, 0.06);
}

.board-frame__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  border-bottom: 1px solid rgba(0, 180, 255, 0.15);
  flex-shrink: 0;
}

.board-frame__tabs {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.board-frame__tab {
  padding: 3px 10px;
  font-size: 11px;
  color: #6d8fb3;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.board-frame__tab--active {
  color: #e8f4ff;
  border-color: rgba(0, 180, 255, 0.45);
  background: rgba(0, 120, 255, 0.12);
}

.board-frame__extra {
  font-size: 10px;
  color: #5a7a9a;
}

.board-frame__body {
  flex: 1;
  min-height: 0;
  padding: 8px 10px;
  overflow: hidden;
}

.board-frame__corner {
  position: absolute;
  width: 12px;
  height: 12px;
  border-color: #00c8ff;
  border-style: solid;
  pointer-events: none;
}

.board-frame__corner--tl { top: -1px; left: -1px; border-width: 2px 0 0 2px; }
.board-frame__corner--tr { top: -1px; right: -1px; border-width: 2px 2px 0 0; }
.board-frame__corner--bl { bottom: -1px; left: -1px; border-width: 0 0 2px 2px; }
.board-frame__corner--br { bottom: -1px; right: -1px; border-width: 0 2px 2px 0; }
</style>
