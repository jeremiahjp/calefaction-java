package com.calefaction.features.chat;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VideoDownloadService {
    private static final Logger log = LoggerFactory.getLogger(VideoDownloadService.class);

    public CompletableFuture<File> downloadVideo(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File tempFile = File.createTempFile("video_", ".mp4");
                // Note: yt-dlp might change the extension depending on format, but we force mp4
                String outputPath = tempFile.getAbsolutePath();

                // Remove the empty temp file created by Java so yt-dlp can write it cleanly
                tempFile.delete();

                ProcessBuilder pb = new ProcessBuilder(
                        "yt-dlp",
                        "--no-playlist",
                        "--max-filesize", "25M",
                        "-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best",
                        "--merge-output-format", "mp4",
                        "-o", outputPath,
                        url
                );

                pb.redirectErrorStream(true);
                Process process = pb.start();

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new RuntimeException("yt-dlp exited with code " + exitCode);
                }

                File downloadedFile = new File(outputPath);
                if (!downloadedFile.exists()) {
                    throw new RuntimeException("yt-dlp succeeded but file was not found at " + outputPath);
                }

                return downloadedFile;
            } catch (Exception e) {
                log.error("Failed to download video from URL: {}", url, e);
                throw new RuntimeException("Failed to download video", e);
            }
        });
    }
}
