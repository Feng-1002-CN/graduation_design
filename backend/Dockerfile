FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache ffmpeg

WORKDIR /app
COPY target/competition-assistant-1.0.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

