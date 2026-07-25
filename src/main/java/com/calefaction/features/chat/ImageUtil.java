package com.calefaction.features.chat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ImageUtil {

    private static final Logger log = LoggerFactory.getLogger(ImageUtil.class);
    private final HttpClient httpClient;

    public ImageUtil() {
        this.httpClient = HttpClient.newBuilder().build();
    }

    /**
     * Fetches an image from a URL and returns it as a base64 encoded string.
     *
     * @param imageUrl The URL of the image to fetch
     * @return Base64 encoded image data, or null if fetch fails
     */
    public String fetchImageAsBase64(String imageUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                String base64 = Base64.getEncoder().encodeToString(response.body());
                log.info("[ImageUtil] Successfully fetched image - URL: {}, Size: {} bytes, Base64 length: {}",
                        imageUrl, response.body().length, base64.length());
                return base64;
            } else {
                log.error("[ImageUtil] Failed to fetch image from URL: {} - Status: {}", imageUrl,
                        response.statusCode());
                return null;
            }
        } catch (IOException | InterruptedException e) {
            log.error("Error fetching image from URL: {}", imageUrl, e);
            return null;
        }
    }

    /**
     * Determines the MIME type from a URL based on file extension.
     *
     * @param imageUrl The URL of the image
     * @return MIME type string (e.g., "image/png", "image/jpeg")
     */
    public String getMimeType(String imageUrl) {
        String lowerUrl = imageUrl.toLowerCase();
        if (lowerUrl.endsWith(".png")) {
            return "image/png";
        } else if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerUrl.endsWith(".webp")) {
            return "image/webp";
        } else if (lowerUrl.endsWith(".gif")) {
            return "image/gif";
        }
        return "image/jpeg"; // Default fallback
    }
}
