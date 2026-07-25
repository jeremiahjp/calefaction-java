package com.calefaction.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class MediaDownloadService {

    private static final Logger log = LoggerFactory.getLogger(MediaDownloadService.class);
    private final WebClient downloadClient;

    public MediaDownloadService() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024))
                .build();
        this.downloadClient = WebClient.builder()
                .exchangeStrategies(strategies)
                .build();
    }

    public Mono<byte[]> download(String url) {
        return downloadClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(byte[].class)
                .onErrorResume(e -> {
                    log.error("Failed to download media from URL: {}", url, e);
                    return Mono.empty();
                });
    }
}
