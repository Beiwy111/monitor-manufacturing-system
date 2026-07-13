-- =============================================================
-- Agent 全流程编排：可暂停 / 可恢复 / 全程可审计
-- 一次 run = 一条 agent_flow_run；每个模块闸门 = 一条 agent_flow_step
-- 目标库：display_manufacturing (MySQL 8, utf8mb4)
-- =============================================================

DROP TABLE IF EXISTS `agent_flow_step`;
DROP TABLE IF EXISTS `agent_flow_run`;

-- 流程实例（一次自动推进）
CREATE TABLE `agent_flow_run` (
  `flow_id`        bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '流程实例ID',
  `flow_no`        varchar(50)  NOT NULL COMMENT '流程编号',
  `template_code`  varchar(50)  NOT NULL COMMENT '流程模板：如 ORDER_TO_SETTLE',
  `goal`           varchar(500) DEFAULT NULL COMMENT '自然语言目标',
  `status`         varchar(20)  NOT NULL DEFAULT 'RUNNING' COMMENT '状态',
  `current_step_no` int         NOT NULL DEFAULT 1 COMMENT '当前推进到的步骤序号',
  `context_json`   json         DEFAULT NULL COMMENT '累积上下文快照',
  `created_by`     bigint unsigned DEFAULT NULL COMMENT '发起人用户ID',
  `created_at`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`flow_id`),
  UNIQUE KEY `uk_agent_flow_no` (`flow_no`),
  KEY `idx_agent_flow_status` (`status`),
  CONSTRAINT `chk_agent_flow_status`
    CHECK (`status` IN ('RUNNING','PAUSED','COMPLETED','TERMINATED','FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent流程实例';

-- 流程步骤（每个模块的一次"提议→闸门→执行"）
CREATE TABLE `agent_flow_step` (
  `step_id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '步骤ID',
  `flow_id`          bigint unsigned NOT NULL COMMENT '所属流程实例',
  `step_no`          int          NOT NULL COMMENT '步骤序号(从1起)',
  `module`           varchar(20)  NOT NULL COMMENT '模块',
  `action_code`      varchar(60)  NOT NULL COMMENT '动作/工具名，如 quality.fail',
  `title`            varchar(200) DEFAULT NULL COMMENT '步骤标题',
  `status`           varchar(20)  NOT NULL DEFAULT 'PENDING' COMMENT '步骤状态',
  `reason`           varchar(1000) DEFAULT NULL COMMENT 'Agent 给出的理由',
  `proposal_json`    json         DEFAULT NULL COMMENT 'Agent 提议的动作参数',
  `final_params_json` json        DEFAULT NULL COMMENT '人工确认/修改后的最终参数',
  `decision`         varchar(20)  DEFAULT NULL COMMENT '人工决策',
  `decision_by`      bigint unsigned DEFAULT NULL COMMENT '决策人用户ID',
  `decision_at`      datetime     DEFAULT NULL COMMENT '决策时间',
  `result_json`      json         DEFAULT NULL COMMENT '执行后端接口的返回',
  `error_msg`        varchar(1000) DEFAULT NULL COMMENT '执行失败信息',
  `created_at`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`step_id`),
  KEY `idx_agent_step_flow` (`flow_id`,`step_no`),
  CONSTRAINT `fk_agent_step_flow` FOREIGN KEY (`flow_id`) REFERENCES `agent_flow_run` (`flow_id`) ON DELETE CASCADE,
  CONSTRAINT `chk_agent_step_module`
    CHECK (`module` IN ('PURCHASE','QUALITY','EQUIPMENT','AFTERSALES','COST')),
  CONSTRAINT `chk_agent_step_status`
    CHECK (`status` IN ('PENDING','PROPOSED','APPROVED','MODIFIED','EXECUTED','SKIPPED','REJECTED','FAILED')),
  CONSTRAINT `chk_agent_step_decision`
    CHECK (`decision` IS NULL OR `decision` IN ('APPROVE','MODIFY','SKIP','TERMINATE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent流程步骤';

-- =============================================================
-- 演示种子：一条"暂停在质检环、等待人工确认"的流程
-- 展示：第1环已执行 → 第2环 Agent 已提议、正卡在闸门 → 后续待推进
-- =============================================================
INSERT INTO `agent_flow_run`
  (`flow_no`,`template_code`,`goal`,`status`,`current_step_no`,`context_json`,`created_by`)
VALUES
  ('AF20260711001','ORDER_TO_SETTLE',
   '工单 WO-20260711 全流程推进：补料→检验→保障→结算',
   'PAUSED', 2,
   JSON_OBJECT('workOrderNo','WO-20260711','materialCode','MAT-003'),
   1);

SET @fid := LAST_INSERT_ID();

INSERT INTO `agent_flow_step`
  (`flow_id`,`step_no`,`module`,`action_code`,`title`,`status`,`reason`,`proposal_json`,`final_params_json`,`decision`,`decision_by`,`decision_at`,`result_json`)
VALUES
  (@fid,1,'PURCHASE','purchase.generate','驱动IC 缺料补料','EXECUTED',
   '驱动IC 净缺料 1617，绑定供应商芯联电子，建议一键生成采购单',
   JSON_OBJECT('requirementIds', JSON_ARRAY(1)),
   JSON_OBJECT('requirementIds', JSON_ARRAY(1)),
   'APPROVE',1,NOW(),
   JSON_OBJECT('purchaseOrderNo','PO...','totalAmount',0)),
  (@fid,2,'QUALITY','quality.fail','来料检发现亮点不良','PROPOSED',
   '抽检 5 片，发现 1 片亮点不良，建议判不通过并登记不良品(轻微)',
   JSON_OBJECT('inspectionId',2,'defectReason','屏体亮点','defectQuantity',1,'severity','MINOR'),
   NULL,NULL,NULL,NULL,NULL),
  (@fid,3,'COST','cost.confirm','工单成本结算','PENDING',
   NULL,NULL,NULL,NULL,NULL,NULL,NULL);
