FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/storage-service-1.0.1.jar /app/app.jar
EXPOSE 8085
ENTRYPOINT ["sh","-c","java -jar /app/app.jar"]
