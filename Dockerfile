FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN apk add --no-cache libstdc++ gcompat ffmpeg python3 wget \
    && wget https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -O /usr/local/bin/yt-dlp \
    && chmod a+rx /usr/local/bin/yt-dlp
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
