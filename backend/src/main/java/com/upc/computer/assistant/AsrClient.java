package com.upc.computer.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.computer.common.BusinessException;
import com.upc.computer.config.AiProperties;
import com.upc.computer.config.AssistantProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 语音识别客户端 —— 阿里云百炼「语音识别」模型（qwen3-asr-flash），OpenAI 兼容 /chat/completions。
 * 复用 ai.api-key / ai.base-url（与视觉 OCR 同一把 Bearer key、同一端点），把音频以
 * data:audio/&lt;fmt&gt;;base64 放入 input_audio 内容块，返回纯转写文本。
 *
 * mock=true 时不调真实接口，返回轮换样例文本，便于无网络联调整条链路。
 */
@Component
public class AsrClient {

    private static final Logger log = LoggerFactory.getLogger(AsrClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** mock 模式轮换的样例命令，覆盖查询/受理/解决(缺方案)/关闭等分支 */
    private static final String[] MOCK_SAMPLES = {
        "查一下售后的KPI统计",
        "把星辰科技那个色差的单子受理一下",
        "标记解决深圳明图的亮线问题，方案是已重新质检并安排退换货",
        "关闭广州绿洲那个接口故障的案例"
    };
    private final AtomicInteger mockCursor = new AtomicInteger(0);

    private final AiProperties aiProps;
    private final AssistantProperties props;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    public AsrClient(AiProperties aiProps, AssistantProperties props) {
        this.aiProps = aiProps;
        this.props = props;
    }

    /**
     * 识别一段音频，返回文本。
     * @param audioBytes 录音字节（建议 16k 单声道 WAV）
     * @param mimeType   浏览器上传的 content-type（audio/wav、audio/webm...），用于推断格式
     */
    public String recognize(byte[] audioBytes, String mimeType) {
        AssistantProperties.Asr asr = props.getAsr();
        if (asr.isMock()) {
            String text = MOCK_SAMPLES[Math.floorMod(mockCursor.getAndIncrement(), MOCK_SAMPLES.length)];
            log.info("[ASR-MOCK] 返回样例文本：{}", text);
            return text;
        }
        if (audioBytes == null || audioBytes.length == 0) {
            throw new BusinessException("录音为空");
        }
        if (audioBytes.length < 1000) {
            // WAV 头约 44 字节；小于 1KB 基本是没录到声音
            throw new BusinessException("录音过短或没录到声音，请点麦克风说完再点停止");
        }
        String apiKey = firstNonBlank(asr.getApiKey(), aiProps.getApiKey());
        String baseUrl = firstNonBlank(asr.getBaseUrl(), aiProps.getBaseUrl());
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException("未配置语音识别 api-key（assistant.asr.api-key 或 ai.api-key）");
        }

        try {
            String fmt = audioFormat(mimeType);
            String dataUrl = "data:audio/" + fmt + ";base64," + Base64.getEncoder().encodeToString(audioBytes);

            Map<String, Object> inputAudio = new LinkedHashMap<>();
            inputAudio.put("data", dataUrl);
            Map<String, Object> audioPart = new LinkedHashMap<>();
            audioPart.put("type", "input_audio");
            audioPart.put("input_audio", inputAudio);
            Map<String, Object> userMsg = new LinkedHashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", List.of(audioPart));
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", asr.getModel());
            body.put("messages", List.of(userMsg));

            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(40))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .build();
            HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            String respBody = new String(resp.body(), StandardCharsets.UTF_8);
            if (resp.statusCode() / 100 != 2) {
                throw new BusinessException("语音识别接口异常 HTTP " + resp.statusCode() + "：" + truncate(respBody, 300));
            }
            JsonNode content = MAPPER.readTree(respBody).path("choices").path(0).path("message").path("content");
            String text = content.isTextual() ? content.asText("") : "";
            return text.trim();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[ASR] 调用百炼语音识别失败", e);
            throw new BusinessException("语音识别调用失败：" + e.getMessage());
        }
    }

    /** content-type -> 百炼可识别的音频格式 */
    private String audioFormat(String mimeType) {
        if (mimeType == null) return "wav";
        String m = mimeType.toLowerCase();
        if (m.contains("wav")) return "wav";
        if (m.contains("webm")) return "webm";
        if (m.contains("ogg") || m.contains("opus")) return "ogg";
        if (m.contains("mpeg") || m.contains("mp3")) return "mp3";
        if (m.contains("mp4") || m.contains("m4a") || m.contains("aac")) return "mp4";
        return "wav";
    }

    private String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
