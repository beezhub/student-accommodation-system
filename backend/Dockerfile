# Build stage
FROM gradle:8.11.1-jdk21-alpine AS build

WORKDIR /app

# Copy Gradle files for dependency caching
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Download dependencies (this layer will be cached if dependencies don't change)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Build the application (skip tests for faster builds, run tests in CI/CD)
RUN gradle bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user to run the application
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the built JAR from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Change ownership to the spring user
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring:spring

# Expose the application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
