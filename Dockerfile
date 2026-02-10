# Builder stage
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app
COPY . .
# adjust build command if your project uses a different Gradle task
RUN gradle bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar /app/app.jar
EXPOSE 9002
ENTRYPOINT ["java","-jar","/app/app.jar"]
