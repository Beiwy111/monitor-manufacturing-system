package com.upc.computer.controller;

import com.upc.computer.common.Result;
import com.upc.computer.service.YoloVisionClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/quality/vision")
public class QualityVisionController {

    @Autowired
    private YoloVisionClient yoloVisionClient;

    /** YOLO 外观缺陷检测：上传图片，返回缺陷类型、置信度与标注图 */
    @PostMapping("/detect")
    public Result<Map<String, Object>> detect(@RequestParam("file") MultipartFile file) {
        return Result.success(yoloVisionClient.detect(file));
    }
}
