# ============================================================
# Multi-Stage Dockerfile for Render Deployment
# Uses the project's Maven Wrapper and Eclipse Temurin JDK 21
# ============================================================

# Stage 1: Build the Spring Boot application using Maven Wrapper
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Install curl and unzip required by Maven Wrapper only-script type
RUN apt-get update && apt-get install -y --no-install-recommends curl unzip \
    && rm -rf /var/lib/apt/lists/*

# Copy Maven Wrapper files
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Normalize line endings (CRLF to LF) and grant execution permissions
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Pre-fetch project dependencies for Docker layer caching
RUN ./mvnw dependency:go-offline -B

# Copy application source code and compile fat JAR
COPY src/ ./src/
RUN ./mvnw clean package -DskipTests

# Stage 2: Minimal production runtime with Eclipse Temurin JRE 21
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the compiled JAR artifact from the build stage
COPY --from=build /app/target/*.jar app.jar

# Render dynamically sets the PORT environment variable (defaulting to 8080)
ENV PORT=8080
EXPOSE 8080

# Execute Spring Boot application
ENTRYPOINT ["sh", "-c", "java -Djava.net.preferIPv4Stack=true -Dserver.port=${PORT} -jar /app/app.jar"]
