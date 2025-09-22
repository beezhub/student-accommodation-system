# Stage 1: Build the application
FROM gradle:jdk21-alpine AS builder
WORKDIR /app

# Copy only the files needed for dependency resolution first
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
COPY gradlew .
RUN chmod +x ./gradlew

# Download dependencies - this layer will be cached if dependencies don't change
RUN ./gradlew downloadDependencies --no-daemon || return 0

# Copy source code
COPY src ./src

# Build the application
RUN ./gradlew bootJar --no-daemon --parallel --build-cache -x test \
  && find build/libs/ -type f -name '*.jar' -exec ls -l {} \;

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create directory and set permissions before switching user
RUN mkdir -p /app/libs/ && \
  addgroup -S spring && \
  adduser -S spring -G spring && \
  chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Copy jar
COPY --from=builder --chown=spring:spring /app/build/libs/ /app/libs/
RUN find /app/libs/ -type f -name '*.jar' -exec mv {} /app/app.jar \;

EXPOSE 8080

# Configure health check
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]