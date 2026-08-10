FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

RUN apk add --no-cache maven

COPY pom.xml .

# Pre-download dependencies using BuildKit cache mount so they survive rebuilds
# and don't require network access on subsequent builds.
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B --quiet

COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B --offline

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
