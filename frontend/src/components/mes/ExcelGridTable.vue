<template>
  <div class="excel-grid-wrap" :class="{ 'excel-grid-wrap--compact': compact }">
    <table class="excel-grid">
      <thead>
        <tr>
          <th v-if="showRowNo" class="excel-grid__row-no">#</th>
          <th
            v-for="col in columns"
            :key="col.prop"
            :class="['excel-grid__th', col.align ? `excel-grid__th--${col.align}` : '']"
            :style="col.width ? { width: col.width, minWidth: col.width } : {}"
          >
            {{ col.label }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="!data?.length">
          <td :colspan="columns.length + (showRowNo ? 1 : 0)" class="excel-grid__empty">暂无数据</td>
        </tr>
        <tr
          v-for="(row, ri) in data"
          :key="rowKey(row, ri)"
          class="excel-grid__tr"
          :class="{
            'excel-grid__tr--selected': isSelected(row, ri),
            'excel-grid__tr--stripe': ri % 2 === 1,
            'excel-grid__tr--selectable': selectable
          }"
          @click="onRowClick(row, ri)"
        >
          <td v-if="showRowNo" class="excel-grid__row-no">{{ ri + 1 }}</td>
          <td
            v-for="col in columns"
            :key="col.prop"
            :class="['excel-grid__td', col.align ? `excel-grid__td--${col.align}` : '']"
          >
            <slot :name="col.prop" :row="row" :index="ri">
              {{ formatCell(row, col) }}
            </slot>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
const props = defineProps({
  columns: { type: Array, default: () => [] },
  data: { type: Array, default: () => [] },
  rowKeyField: { type: String, default: 'id' },
  selectedKey: { type: [String, Number], default: '' },
  showRowNo: { type: Boolean, default: true },
  compact: { type: Boolean, default: false },
  selectable: { type: Boolean, default: false }
})

const emit = defineEmits(['row-click', 'update:selectedKey'])

function rowKey(row, index) {
  return row?.[props.rowKeyField] ?? index
}

function isSelected(row, index) {
  if (!props.selectable || !props.selectedKey) return false
  const key = row?.[props.rowKeyField] ?? row?.key ?? index
  return String(key) === String(props.selectedKey)
}

function formatCell(row, col) {
  const val = row?.[col.prop]
  if (val == null || val === '') return col.emptyText ?? '—'
  if (col.formatter) return col.formatter(val, row)
  return val
}

function onRowClick(row, index) {
  if (props.selectable) {
    const key = row?.[props.rowKeyField] ?? row?.key ?? index
    emit('update:selectedKey', key)
  }
  emit('row-click', row, index)
}
</script>

<style scoped>
.excel-grid-wrap {
  overflow: auto;
  border: 1px solid #b4b4b4;
  border-radius: 2px;
  background: #fff;
  box-shadow: inset 0 0 0 1px #e8e8e8;
}

.excel-grid-wrap--compact .excel-grid__th,
.excel-grid-wrap--compact .excel-grid__td {
  padding: 4px 8px;
  font-size: 12px;
}

.excel-grid {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  font-family: var(--layout-font);
  font-size: 13px;
  color: #000;
}

.excel-grid__th {
  background: linear-gradient(180deg, #f9fafb 0%, #e8ecf1 100%);
  border: 1px solid #b4b4b4;
  padding: 7px 10px;
  font-weight: 600;
  color: #1f2937;
  text-align: center;
  white-space: nowrap;
  position: sticky;
  top: 0;
  z-index: 1;
}

.excel-grid__th--left { text-align: left; }
.excel-grid__th--right { text-align: right; }

.excel-grid__td {
  border: 1px solid #d4d4d4;
  padding: 6px 10px;
  background: #fff;
  vertical-align: middle;
  word-break: break-word;
}

.excel-grid__td--left { text-align: left; }
.excel-grid__td--right { text-align: right; }
.excel-grid__td--center { text-align: center; }

.excel-grid__row-no {
  width: 36px;
  min-width: 36px;
  text-align: center;
  background: #f3f4f6;
  color: #6b7280;
  font-size: 12px;
  border: 1px solid #d4d4d4;
  padding: 6px 4px;
}

.excel-grid__tr--stripe .excel-grid__td:not(.excel-grid__row-no) {
  background: #fafbfc;
}

.excel-grid__tr--selected .excel-grid__td:not(.excel-grid__row-no) {
  background: #e2f0d9 !important;
  outline: 2px solid #217346;
  outline-offset: -2px;
}

.excel-grid__tr {
  cursor: default;
}

.excel-grid__tr--selectable {
  cursor: pointer;
}

.excel-grid__tr--selectable:hover .excel-grid__td:not(.excel-grid__row-no) {
  background: #f5f9fc;
}

.excel-grid__empty {
  text-align: center;
  color: #9ca3af;
  padding: 24px;
  background: #fafafa;
}
</style>
