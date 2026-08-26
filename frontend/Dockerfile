# syntax=docker/dockerfile:1

FROM nginx:1.31.4-alpine3.24@sha256:db35bfc6b2951e7f8a72db5db120288c127ffaeeb4a6d4b95a26fead017d5913

ARG GIT_SHA=unknown
ARG BUILD_DATE=unknown

LABEL org.opencontainers.image.title="TravelMate frontend" \
      org.opencontainers.image.description="TravelMate Vue frontend served by Nginx" \
      org.opencontainers.image.source="https://github.com/Miracle060811/Software_engineering" \
      org.opencontainers.image.revision="${GIT_SHA}" \
      org.opencontainers.image.created="${BUILD_DATE}"

RUN apk upgrade --no-cache

COPY nginx.conf /etc/nginx/nginx.conf
COPY --chown=101:101 dist/ /usr/share/nginx/html/

USER 101:101
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/healthz || exit 1
