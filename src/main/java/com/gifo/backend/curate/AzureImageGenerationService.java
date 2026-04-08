package com.gifo.backend.curate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AzureImageGenerationService {

    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.builder()
            .requestFactory(new ReactorClientHttpRequestFactory(
                    HttpClient.create()
                            .responseTimeout(Duration.ofMinutes(5))
            ))
            .build();

    @Value("${AZURE_OPENAI_IMAGE_GENERATION_URL:}")
    private String imageGenerationUrl;

    @Value("${AZURE_OPENAI_API_KEY:}")
    private String apiKey;

    @Value("${AZURE_OPENAI_IMAGE_MODEL:}")
    private String imageModel;

    @Value("${AZURE_OPENAI_IMAGE_SIZE:512x512}")
    private String imageSize;

    public String generateImageUrl(String prompt) {
        if (!StringUtils.hasText(imageGenerationUrl) || !StringUtils.hasText(apiKey)) {
            log.warn("[Curate-Image] image generation 설정 누락: AZURE_OPENAI_IMAGE_GENERATION_URL/API_KEY");
            return null;
        }

        try {
            log.info("[Curate-Image] 이미지 생성 요청 시작 - endpoint={}, model={}, promptLength={}",
                    imageGenerationUrl, imageModel, prompt != null ? prompt.length() : 0);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("prompt", prompt);
            body.put("size", imageSize);

            // deployment 경로를 직접 쓰는 경우 model이 필수는 아니지만, 설정돼 있으면 함께 전달
            if (StringUtils.hasText(imageModel)) {
                body.put("model", imageModel);
            }

            // FLUX 배포는 b64_json 전용 응답만 지원하므로 response_format을 지정하지 않음
            String responseText = requestImage(body);

            if (!StringUtils.hasText(responseText)) {
                log.warn("[Curate-Image] 이미지 생성 응답이 비어 있음");
                return null;
            }

            JsonNode root = objectMapper.readTree(responseText);
            JsonNode data = root.path("data");
            if (data.isArray() && !data.isEmpty()) {
                JsonNode first = data.get(0);
                String url = first.path("url").asText("");
                if (StringUtils.hasText(url)) {
                    log.info("[Curate-Image] 이미지 생성 성공 - url={}", url);
                    return url;
                }
                String b64 = first.path("b64_json").asText("");
                if (StringUtils.hasText(b64)) {
                    String dataUri = "data:image/png;base64," + b64;
                    log.info("[Curate-Image] 이미지 생성 성공 - b64_json 수신(data-uri 변환), size={}", b64.length());
                    return dataUri;
                }
            }

            log.warn("[Curate-Image] 이미지 URL 추출 실패 - response={}", responseText);
            return null;
        } catch (Exception e) {
            log.warn("[Curate-Image] 이미지 생성 요청 실패 - message={}", e.getMessage(), e);
            return null;
        }
    }

    private String requestImage(Map<String, Object> body) {
        return restClient.post()
                .uri(imageGenerationUrl)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("api-key", apiKey)
                .body(body)
                .retrieve()
                .body(String.class);
    }
}

