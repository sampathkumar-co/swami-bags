package com.swamibags.catalog.ai;

import com.swamibags.catalog.config.AppProperties;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class OpenAiImageService {
    private final AppProperties properties;
    private final RestClient restClient;

    public OpenAiImageService(AppProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder.baseUrl("https://api.openai.com").build();
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
            String message = e.getResponseBodyAsString();
            if (message.length() > 500) {
                message = message.substring(0, 500);
            }
            throw new AiUnavailableException("OpenAI image generation failed: " + message, e);
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
