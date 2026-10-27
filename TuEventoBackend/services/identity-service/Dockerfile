# Contexto de build: TuEventoBackend/services (necesita tuevento-common, que no está publicado)
#   docker build -f identity-service/Dockerfile -t jhampier23/tu-evento:identity-vX.Y.Z TuEventoBackend/services

# ── Build stage ───────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build
RUN apk add --no-cache maven
COPY tuevento-common ./tuevento-common
COPY identity-service ./identity-service
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -B -f tuevento-common/pom.xml install -DskipTests && \
    mvn -q -B -f identity-service/pom.xml package -DskipTests

# ── Runtime stage ─────────────────────────────────────────────────────────────
# Alpine trae wget (busybox) para el healthcheck del compose
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S identity && adduser -S identity -G identity
COPY --from=builder /build/identity-service/target/identity-service-*.jar app.jar
USER identity
EXPOSE 8088
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8088/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
