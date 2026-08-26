# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jre-jammy@sha256:eebd356ad7358b7094758e5787a6726f332917cfd56feab6457c56dab895cdbf

ARG GIT_SHA=unknown
ARG BUILD_DATE=unknown

LABEL org.opencontainers.image.title="TravelMate backend" \
      org.opencontainers.image.description="TravelMate Spring Boot API" \
      org.opencontainers.image.source="https://github.com/Miracle060811/Software_engineering" \
      org.opencontainers.image.revision="${GIT_SHA}" \
      org.opencontainers.image.created="${BUILD_DATE}"

WORKDIR /app

RUN groupadd --gid 10001 travelmate \
    && useradd --uid 10001 --gid 10001 --no-create-home --shell /usr/sbin/nologin travelmate

COPY --chown=10001:10001 target/*.jar /app/travelmate.jar

USER 10001:10001
EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Djava.io.tmpdir=/tmp"

ENTRYPOINT ["java", "-jar", "/app/travelmate.jar"]
