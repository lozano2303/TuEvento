# ============================================================
# Dockerfile — TuEvento Backend (Spring Boot)
# Multi-stage build optimizado para producción en Render
# ============================================================

# ── Etapa 1: Build ─────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Instalar Maven
RUN apk add --no-cache maven

# Copiar descriptor de dependencias primero para aprovechar
# la caché de capas de Docker: si pom.xml no cambia,
# Maven no vuelve a descargar las dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

# Copiar el código fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests -B --no-transfer-progress

# ── Etapa 2: Runtime ───────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Crear usuario no-root para mayor seguridad en producción
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copiar el JAR compilado desde la etapa de build
COPY --from=builder /app/target/*.jar app.jar

# Ajustar permisos
RUN chown appuser:appgroup app.jar

USER appuser

# Render inyecta la variable PORT en tiempo de ejecución.
# Spring Boot la lee como SERVER_PORT. Se expone 8080 como
# documentación; el puerto real lo define Render vía env var.
EXPOSE 8080

# Flags JVM para producción:
#   -XX:+UseContainerSupport     → lee los límites de CPU/RAM del contenedor
#   -XX:MaxRAMPercentage=75.0    → usa hasta el 75% de la RAM disponible para el heap
#                                   (deja margen para metaspace, threads, etc.)
#   -Djava.security.egd=...      → evita bloqueos de SecureRandom en contenedores sin /dev/random
#   -Dspring.profiles.active=prod → activa el perfil de producción
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}", \
  "-jar", "app.jar"]
