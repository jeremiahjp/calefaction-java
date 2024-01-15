package com.jp.calefaction.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class UploadImageService {

    private static final String CLIENT_ID = "b621e23eaa31500";
    private static final String IMGUR_UPLOAD_API_URL = "https://api.imgur.com/3/image";
    private final WebClient webClient;

    public UploadImageService() {
        this.webClient = WebClient.builder()
                .baseUrl(IMGUR_UPLOAD_API_URL)
                .defaultHeader("Authorization", "Client-ID " + CLIENT_ID)
                .build();
    }

    public Mono<String> uploadImage(String imagePath) throws IOException {
        Path path = Paths.get(imagePath);
        byte[] imageBytes = Files.readAllBytes(path);
        String encodedImage = Base64.getEncoder().encodeToString(imageBytes);
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("image", new FileSystemResource(imagePath));

        return webClient
                .post()
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .bodyToMono(String.class)
                .map(this::extractImageUrl);
    }

    private String extractImageUrl(String responseBody) {
        JSONObject jsonObject = new JSONObject(responseBody);
        return jsonObject.getJSONObject("data").getString("link");
    }
}
