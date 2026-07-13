package com.upc.computer.controller;

import com.upc.computer.assistant.AsrClient;
import com.upc.computer.assistant.AssistantService;
import com.upc.computer.common.BusinessException;
import com.upc.computer.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 全 MES 语音助手编排接口（原售后专用入口升级，旧路径 /afterSales/assistant 保留兼容）。
 * ① /asr        语音 → 文本（阿里云一句话识别，mock 模式返回样例）
 * ② /interpret  文本 → 意图（读→直接应答；写→返回待确认提议；body.module 为当前页面模块，供 NLU 意图偏置）
 * ③ /execute    人工闸门决策 → 执行真实业务接口（售后专用 Service 或 MesWorkflowService）
 *
 * 也可直接调 /interpret 用文本驱动，无需录音，便于联调。
 */
@RestController
@RequestMapping({"/assistant", "/afterSales/assistant"})
public class AssistantController {

    @Autowired private AsrClient asrClient;
    @Autowired private AssistantService assistant;

    @PostMapping("/asr")
    public Result<Map<String, Object>> asr(@RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) throw new BusinessException("录音为空");
        String text = asrClient.recognize(file.getBytes(), file.getContentType());
        return Result.success(Map.of("text", text));
    }

    @PostMapping("/interpret")
    public Result<Map<String, Object>> interpret(@RequestBody Map<String, Object> body) {
        String sessionId = str(body, "sessionId");
        String module = str(body, "module");            // 当前页面模块（device/production/...），意图偏置用
        String text = str(body, "text");
        return Result.success(assistant.interpret(sessionId, module, text));
    }

    @PostMapping("/execute")
    public Result<Map<String, Object>> execute(@RequestBody Map<String, Object> body) {
        String proposalId = str(body, "proposalId");
        String decision = str(body, "decision");        // APPROVE / MODIFY / SKIP
        String operator = str(body, "operator");        // 前端带当前登录用户名
        String roleKey = str(body, "roleKey");          // 前端带当前角色，MES 通用动作执行时透传
        if (proposalId.isBlank()) throw new BusinessException("proposalId 不能为空");
        return Result.success(assistant.execute(proposalId, decision, body.get("finalParams"), operator, roleKey));
    }

    /** 跨模块协办通知列表（GlobalBusinessMonitor 轮询，按角色并入通知中心；target 过滤目标模块，可空） */
    @GetMapping("/notices")
    public Result<List<Map<String, Object>>> notices(@RequestParam(required = false) String target) {
        return Result.success(assistant.listNotices(target));
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : "";
    }
}
