-- 扩展状态 CHECK 约束，与 MesWorkflowService 工作流一致（在 display_manufacturing 库执行）
USE display_manufacturing;

ALTER TABLE customer_order DROP CHECK chk_customer_order_audit_status;
ALTER TABLE customer_order ADD CONSTRAINT chk_customer_order_audit_status CHECK (
  audit_status IN ('PENDING','APPROVED','REJECTED','CANCELLED','PLAN_PENDING','PLANNED','PRODUCING','SHIPPED')
);

ALTER TABLE production_plan DROP CHECK chk_production_plan_status;
ALTER TABLE production_plan ADD CONSTRAINT chk_production_plan_status CHECK (
  plan_status IN ('DRAFT','PUBLISHED','SUBMITTED','EXECUTING','COMPLETED','CANCELLED','RELEASED','RUNNING')
);

ALTER TABLE work_order DROP CHECK chk_work_order_status;
ALTER TABLE work_order ADD CONSTRAINT chk_work_order_status CHECK (
  status IN ('DRAFT','RELEASED','DISPATCHED','PRODUCING','QC_PENDING','COMPLETED','CANCELLED','RUNNING','PAUSED')
);

UPDATE customer_order
SET audit_status = 'PLAN_PENDING', updated_at = NOW()
WHERE audit_status = 'APPROVED'
  AND order_id NOT IN (SELECT source_order_id FROM production_plan WHERE source_order_id IS NOT NULL);

UPDATE production_plan
SET plan_status = 'PUBLISHED', updated_at = NOW()
WHERE plan_status IN ('RELEASED', 'RUNNING')
  AND plan_id NOT IN (SELECT plan_id FROM work_order WHERE plan_id IS NOT NULL);

UPDATE production_plan
SET plan_status = 'EXECUTING', updated_at = NOW()
WHERE plan_status = 'RUNNING'
  AND plan_id IN (SELECT plan_id FROM work_order WHERE plan_id IS NOT NULL);

UPDATE work_order SET status = 'PRODUCING', updated_at = NOW() WHERE status = 'RUNNING';

ALTER TABLE dispatch_task DROP CHECK chk_dispatch_status;
ALTER TABLE dispatch_task ADD CONSTRAINT chk_dispatch_status CHECK (
  status IN ('ASSIGNED','ACCEPTED','PRODUCING','RUNNING','QC_PENDING','COMPLETED','CANCELLED')
);

UPDATE dispatch_task SET status = 'PRODUCING', updated_at = NOW()
WHERE status = 'RUNNING';
