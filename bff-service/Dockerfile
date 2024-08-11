FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/bff-service-1.0.0-jar-with-dependencies.jar app.jar
EXPOSE 8002
ENTRYPOINT ["sh", "-c", "java -jar app.jar"]