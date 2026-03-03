# Build stage
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Set a stable, predictable Gradle cache directory inside the image layer.
# This ensures the "resolve dependencies" step is correctly cached by Docker.
ENV GRADLE_USER_HOME=/app/.gradle

# Copy wrapper first — needed to run ./gradlew
COPY gradle ./gradle
COPY gradlew ./
RUN chmod +x gradlew

# Copy only the dependency descriptors so this layer is invalidated only when
# dependencies actually change, not when source code changes.
COPY build.gradle settings.gradle ./

# Pre-resolve all compile + runtime dependencies.
# --no-daemon avoids leaving a daemon process running inside the image layer.
RUN ./gradlew dependencies --no-daemon --quiet

# Copy source code and build the fat JAR (tests run in CI, not here)
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# Runtime stage — use JRE only for a smaller image
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create a non-root user to run the application
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the built JAR from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

RUN chown spring:spring app.jar

USER spring:spring

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
