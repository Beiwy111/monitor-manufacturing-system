package com.upc.computer.controller;

import com.upc.computer.common.BusinessException;
import com.upc.computer.common.Result;
import com.upc.computer.service.VisionReportAiService;
import com.upc.computer.service.YoloVisionClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/quality/vision")
public class QualityVisionController {

    @Autowired
    private YoloVisionClient yoloVisionClient;

    @Autowired
    private VisionReportAiService visionReportAiService;

    /** YOLO 外观缺陷检测：上传图片，返回缺陷类型、置信度与标注图 */
    @PostMapping("/detect")
    public Result<Map<String, Object>> detect(@RequestParam("file") MultipartFile file) {
        return Result.success(yoloVisionClient.detect(file));
    }

    /**
     * 基于已完成的 YOLO 检测结果，一键生成外观检测 AI 报告（千问；失败时模板兜底）。
     * body: { context: {...}, images: [{ name, defect, count, maxConfidence, summary }] }
     */
    @PostMapping("/report")
    public Result<Map<String, Object>> report(@RequestBody Map<String, Object> body) {
        if (body == null) throw new BusinessException("请求体不能为空");
        Object images = body.get("images");
        if (!(images instanceof List<?> list) || list.isEmpty()) {
            throw new BusinessException("请先完成至少一张图片的 YOLO 检测");
        }
        return Result.success(visionReportAiService.generate(body));
    }
}
