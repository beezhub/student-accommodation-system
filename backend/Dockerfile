# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build

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

# Split the jar into Spring Boot's layers (#374): dependencies, the loader, snapshot dependencies,
# and the application's own classes, which are the only part a code change touches. As one fat
# jar, every deploy downloaded the whole 95.6 MB again.
RUN cp build/libs/*.jar app.jar \
 && java -Djarmode=tools -jar app.jar extract --layers --destination extracted

# Runtime stage — use JRE only for a smaller image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user to run the application
RUN addgroup -S spring && adduser -S spring -G spring

# One COPY per layer, least often changed first, so a code-only change replaces only the last.
# Left owned by root and readable by all: the app runs as spring and has no reason to change its
# own code. The old `RUN chown spring:spring app.jar` also wrote a second full copy of the jar
# into a new layer, which every deploy downloaded twice.
COPY --from=build /app/extracted/dependencies/ ./
COPY --from=build /app/extracted/spring-boot-loader/ ./
COPY --from=build /app/extracted/snapshot-dependencies/ ./
COPY --from=build /app/extracted/application/ ./

USER spring:spring

EXPOSE 8080

# Readiness, not the aggregate: the aggregate runs every health contributor, so an SMTP or
# object-storage outage marked a healthy container unhealthy and had it restarted (GH-147).
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/readiness || exit 1

# Without MaxRAMPercentage the JVM takes its own default share of the container limit, which is
# 25% -- about 256MB under the 1G limit in docker-compose. ExitOnOutOfMemoryError makes the
# orchestrator restart the container rather than leaving a JVM thrashing after a heap exhaustion.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError"

# Every timestamp column is TIMESTAMP without time zone and the application writes them with
# LocalDateTime.now() in the JVM default zone, so the zone has to be pinned rather than inherited
# from the base image. Set through the environment rather than a JVM system property, so a
# deployment can override it; a system property would win and make APP_TIMEZONE inert.
ENV TZ=UTC

ENTRYPOINT ["java", "-jar", "app.jar"]
