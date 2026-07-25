# Build Stage
FROM gradle:8.14.0-jdk21-alpine AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .
RUN gradle build --no-daemon -x test

# Run Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache libstdc++ gcompat
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
