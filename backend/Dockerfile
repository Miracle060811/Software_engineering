# syntax=docker/dockerfile:1

FROM eclipse-temurin:21.0.12_8-jre-jammy@sha256:eebd356ad7358b7094758e5787a6726f332917cfd56feab6457c56dab895cdbf

ARG GIT_SHA=unknown
ARG BUILD_DATE=unknown
ARG IMAGE_VERSION=dev

LABEL org.opencontainers.image.title="TravelMate backend" \
      org.opencontainers.image.description="TravelMate Spring Boot API" \
      org.opencontainers.image.source="https://github.com/Miracle060811/Software_engineering" \
      org.opencontainers.image.revision="${GIT_SHA}" \
      org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.version="${IMAGE_VERSION}"

WORKDIR /app

RUN groupadd --gid 10001 travelmate \
    && useradd --uid 10001 --gid 10001 --no-create-home --shell /usr/sbin/nologin travelmate

COPY --chown=10001:10001 target/*.jar /app/travelmate.jar

USER 10001:10001
EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Djava.io.tmpdir=/tmp"

STOPSIGNAL SIGTERM

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD bash -ec 'exec 3<>/dev/tcp/127.0.0.1/8080; printf "GET /actuator/health/liveness HTTP/1.0\r\nHost: 127.0.0.1\r\n\r\n" >&3; IFS= read -r status <&3; [[ "$status" == *" 200 "* ]]'

ENTRYPOINT ["java", "-jar", "/app/travelmate.jar"]
