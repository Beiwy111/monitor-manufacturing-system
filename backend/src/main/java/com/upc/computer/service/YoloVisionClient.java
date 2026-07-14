package com.upc.computer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.computer.common.BusinessException;
import com.upc.computer.config.YoloProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class YoloVisionClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final YoloProperties props;
    private final RestTemplate restTemplate;

    public YoloVisionClient(YoloProperties props) {
        this.props = props;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15_000);
        factory.setReadTimeout(props.getTimeoutSeconds() * 1000);
        this.restTemplate = new RestTemplate(factory);
    }

    public Map<String, Object> detect(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传检测图片");
        }
        if (props.isMock()) {
            return mockResult();
        }
        if (!props.isEnabled()) {
            throw new BusinessException("YOLO 视觉检测未启用");
        }
        try {
            byte[] bytes = file.getBytes();
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg";
            ByteArrayResource resource = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", resource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            String url = props.getBaseUrl().replaceAll("/+$", "") + props.getPredictPath();
            ResponseEntity<String> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(body, headers), String.class);

            String respBody = response.getBody();
            if (respBody == null || respBody.isBlank()) {
                throw new BusinessException("YOLO 服务返回空响应");
            }
            JsonNode root = MAPPER.readTree(respBody);
            if (root.has("detail")) {
                throw new BusinessException("YOLO 推理失败: " + root.path("detail").asText());
            }
            return normalizeResult(root);
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            if (isConnectionError(e)) {
                throw new BusinessException(
                        "YOLO 服务未启动，请先在 Mobile-Phone-Defect 目录运行 start-yolo.bat（端口 8000）");
            }
            throw new BusinessException("YOLO 检测请求失败: " + e.getMessage());
        } catch (Exception e) {
            throw new BusinessException("YOLO 检测失败: " + e.getMessage());
        }
    }

    private boolean isConnectionError(Throwable e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return msg.contains("connection refused")
                || msg.contains("connect")
                || msg.contains("timed out")
                || e.getCause() instanceof java.net.ConnectException;
    }

    private Map<String, Object> normalizeResult(JsonNode root) {
        Map<String, Object> result = new LinkedHashMap<>();
        boolean defect = root.path("defect").asBoolean(false);
        int count = root.path("count").asInt(0);
        double maxConfidence = root.path("maxConfidence").asDouble(0);
        result.put("defect", defect);
        result.put("count", count);
        result.put("maxConfidence", maxConfidence);
        result.put("model", root.path("model").asText("YOLOv8-Seg"));
        result.put("algorithmVersion", root.path("algorithmVersion").asText("SCRATCH_SEG_V1"));
        String encoded = root.path("resultImage").asText("");
        if (!encoded.isBlank()) {
            result.put("resultImage", encoded.startsWith("data:")
                    ? encoded
                    : "data:image/png;base64," + encoded);
        }
        List<Map<String, Object>> detections = new ArrayList<>();
        JsonNode arr = root.path("detections");
        if (arr.isArray()) {
            for (JsonNode node : arr) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("classId", node.path("classId").asInt());
                row.put("className", node.path("className").asText("Scratched"));
                row.put("confidence", node.path("confidence").asDouble(0));
                List<Double> box = new ArrayList<>();
                JsonNode boxNode = node.path("box");
                if (boxNode.isArray()) {
                    boxNode.forEach(v -> box.add(v.asDouble()));
                }
                row.put("box", box);
                detections.add(row);
            }
        }
        result.put("detections", detections);
        result.put("defectType", defect ? "屏幕划痕" : "无缺陷");
        result.put("defectTypeEn", defect ? "Scratched" : "Normal");
        result.put("summary", defect
                ? String.format("检测到 %d 处屏幕划痕，最高置信度 %.0f%%", count, maxConfidence * 100)
                : "未检测到屏幕表面划痕");
        return result;
    }

    private Map<String, Object> mockResult() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("defect", false);
        result.put("count", 0);
        result.put("maxConfidence", 0.0);
        result.put("model", "YOLOv8-Seg (Mock)");
        result.put("algorithmVersion", "MOCK_V1");
        result.put("detections", List.of());
        result.put("defectType", "无缺陷");
        result.put("defectTypeEn", "Normal");
        result.put("summary", "Mock 模式：未检测到缺陷");
        return result;
    }
}
