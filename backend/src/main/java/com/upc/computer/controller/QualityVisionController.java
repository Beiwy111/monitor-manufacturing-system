package com.upc.computer.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.computer.common.BusinessException;
import com.upc.computer.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/quality/vision")
public class QualityVisionController {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper;

    @Value("${quality.vision.url:http://localhost:8000}")
    private String visionUrl;

    public QualityVisionController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostMapping("/detect")
    public Result<Map<String, Object>> detect(@RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) throw new BusinessException("请选择待检测图片");

        String boundary = "----MesVision" + UUID.randomUUID();
        byte[] body = multipartBody(boundary, file);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(visionUrl + "/predict-json"))
                .timeout(Duration.ofSeconds(90))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new BusinessException("AI 外观检测服务未启动，请先启动 YOLO 服务");
        }
        if (response.statusCode() / 100 != 2) throw new BusinessException("AI 外观检测失败：" + response.body());
        return Result.success(objectMapper.readValue(response.body(), new TypeReference<>() {}));
    }

    private byte[] multipartBody(String boundary, MultipartFile file) throws Exception {
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\""
                + (file.getOriginalFilename() == null ? "inspection.jpg" : file.getOriginalFilename()) + "\"\r\n"
                + "Content-Type: " + (file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                + "\r\n\r\n";
        byte[] prefix = header.getBytes(StandardCharsets.UTF_8);
        byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] content = file.getBytes();
        byte[] body = new byte[prefix.length + content.length + suffix.length];
        System.arraycopy(prefix, 0, body, 0, prefix.length);
        System.arraycopy(content, 0, body, prefix.length, content.length);
        System.arraycopy(suffix, 0, body, prefix.length + content.length, suffix.length);
        return body;
    }
}
