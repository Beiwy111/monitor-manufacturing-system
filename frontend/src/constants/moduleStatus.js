const STATUS_TYPES = {
  aftersaleCase: {
    OPEN: 'danger',
    PROCESSING: 'warning',
    RESOLVED: 'primary',
    CLOSED: 'info'
  },
  costSettlement: {
    DRAFT: 'info',
    CONFIRMED: 'primary',
    EXPORTED: 'success'
  },
  qualityInspection: {
    PENDING: 'warning',
    PASSED: 'success',
    FAILED: 'danger',
    RECHECK_REQUIRED: 'warning',
    CLOSED: 'info'
  },
  qualityItem: {
    PASSED: 'success',
    FAILED: 'danger',
    WARNING: 'warning',
    PENDING: 'info'
  },
  defectLevel: {
    MINOR: '',
    MAJOR: 'warning',
    CRITICAL: 'danger'
  },
  costSource: {
    NONCONFORMING_PRODUCT: 'danger',
    AFTER_SALES: 'warning',
    EQUIPMENT_MAINTENANCE: '',
    PURCHASE_RETURN: 'warning',
    WORK_ORDER: 'primary',
    OTHER: 'info'
  },
  purchaseRequirement: {
    PENDING: 'warning',
    SELECTED: 'primary',
    PURCHASED: 'success',
    PART_ARRIVED: 'warning',
    ARRIVED: 'success',
    CANCELLED: 'info'
  },
  equipmentStatus: {
    IDLE: 'info',
    RUNNING: 'success',
    FAULT: 'danger',
    MAINTAINING: 'warning',
    SCRAPPED: 'info'
  },
  alarmLevel: {
    GENERAL: 'info',
    IMPORTANT: 'warning',
    URGENT: 'danger'
  },
  alarmStatus: {
    OPEN: 'danger',
    RECEIVED: 'warning',
    PROCESSING: 'warning',
    CLOSED: 'success',
    CANCELLED: 'info'
  }
}

export function moduleStatusType(group, status, fallback = 'info') {
  return STATUS_TYPES[group]?.[status] ?? fallback
}
