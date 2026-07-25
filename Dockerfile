# Build Stage
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./gradlew build --no-daemon -x test

# Run Stage
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN apk add --no-cache libstdc++ gcompat
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
