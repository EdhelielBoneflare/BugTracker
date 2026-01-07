# Build stage
FROM gradle:8.14-jdk21-alpine AS build
WORKDIR /app

COPY . .

RUN gradle build --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache tzdata
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]