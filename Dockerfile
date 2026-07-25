FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN apk add --no-cache libstdc++ gcompat
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
