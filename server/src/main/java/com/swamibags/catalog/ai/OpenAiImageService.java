package com.swamibags.catalog.ai;

import com.swamibags.catalog.config.AppProperties;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class OpenAiImageService {
    private static final int MAX_ATTEMPTS = 3;

    private final AppProperties properties;
    private final RestClient restClient;

    public OpenAiImageService(AppProperties properties) {
        this.properties = properties;

        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(170));

        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .requestFactory(requestFactory)
                .build();
    }

    public byte[] edit(List<Path> references, String prompt) {
        if (!properties.aiConfigured()) {
            throw new AiUnavailableException("OPENAI_API_KEY is not configured yet.");
        }
        if (references == null || references.isEmpty()) {
            throw new IllegalArgumentException("Upload at least one original product photo first.");
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("model", properties.openAiImageModel());
        body.add("prompt", prompt);
        body.add("quality", properties.openAiImageQuality());
        body.add("size", properties.openAiImageSize());
        body.add("output_format", "jpeg");
        body.add("output_compression", "92");
        references.stream().limit(4).forEach(path -> body.add("image[]", new FileSystemResource(path)));

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                OpenAiImageResponse response = restClient.post()
                        .uri("/v1/images/edits")
                        .header("Authorization", "Bearer " + properties.openAiApiKey())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(body)
                        .retrieve()
                        .body(OpenAiImageResponse.class);

                if (response == null || response.data() == null || response.data().isEmpty()
                        || response.data().getFirst().b64_json() == null) {
                    throw new AiUnavailableException("Image generation completed without returning an image.");
                }
                return Base64.getDecoder().decode(response.data().getFirst().b64_json());
            } catch (RestClientResponseException e) {
                if (attempt < MAX_ATTEMPTS && isRetryable(e)) {
                    sleepBeforeRetry(attempt);
                    continue;
                }
                throw apiFailure(e);
            } catch (RestClientException e) {
                if (attempt < MAX_ATTEMPTS) {
                    sleepBeforeRetry(attempt);
                    continue;
                }
                throw new AiUnavailableException("OpenAI image generation could not be reached after retrying.", e);
            }
        }

        throw new AiUnavailableException("OpenAI image generation failed.");
    }

    private boolean isRetryable(RestClientResponseException exception) {
        return exception.getStatusCode().value() == 429 || exception.getStatusCode().is5xxServerError();
    }

    private AiUnavailableException apiFailure(RestClientResponseException exception) {
        String body = exception.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            body = exception.getStatusText();
        }
        if (body.length() > 500) {
            body = body.substring(0, 500);
        }

        String requestId = exception.getResponseHeaders() == null
                ? null
                : exception.getResponseHeaders().getFirst("x-request-id");
        String suffix = requestId == null || requestId.isBlank() ? "" : " (request " + requestId + ")";
        return new AiUnavailableException("OpenAI image generation failed" + suffix + ": " + body, exception);
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(attempt * 1_000L);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AiUnavailableException("Image generation retry was interrupted.", interrupted);
        }
    }

    public record OpenAiImageResponse(List<ImageData> data) {}
    public record ImageData(String b64_json, String revised_prompt) {}

    public static class AiUnavailableException extends RuntimeException {
        public AiUnavailableException(String message) {
            super(message);
        }

        public AiUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
