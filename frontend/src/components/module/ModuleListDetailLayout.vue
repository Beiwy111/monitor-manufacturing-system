<template>
  <div class="module-list-detail" :style="{ '--module-detail-width': detailWidth }">
    <section class="module-list-detail__list">
      <slot name="list" />
    </section>

    <aside v-if="hasDetail" class="module-list-detail__detail">
      <slot name="detail" />
    </aside>
    <aside v-else class="module-list-detail__detail module-list-detail__detail--empty">
      <slot name="empty">
        <el-empty :description="emptyDescription" />
      </slot>
    </aside>
  </div>
</template>

<script setup>
defineProps({
  hasDetail: { type: Boolean, default: false },
  detailWidth: { type: String, default: '400px' },
  emptyDescription: { type: String, default: '请选择左侧记录查看详情' }
})
</script>

<style scoped>
.module-list-detail {
  display: flex;
  gap: 12px;
  flex: 1;
  min-height: 0;
}

.module-list-detail__list {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.module-list-detail__detail {
  width: var(--module-detail-width);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
}

.module-list-detail__detail--empty {
  align-items: center;
  justify-content: center;
}
</style>
