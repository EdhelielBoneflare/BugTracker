# Build stage
FROM gradle:8.14-jdk21-alpine AS build
WORKDIR /app

# Копируем всё
COPY . .

# Собираем
RUN gradle build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache tzdata
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]