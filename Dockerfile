FROM eclipse-temurin:21-jdk-jammy
LABEL authors="yohannesyimam"

WORKDIR /audio-service
#COPY target/audioService-1.0.0-SNAPSHOT-fat.jar app.jar
ADD https://github.com/JoAbyssinia/audioService/releases/download/v0.1.0/audioService-1.0.0-SNAPSHOT-fat.jar audioService.jar

ENTRYPOINT ["java", "-jar", "audioService.jar"]

