# syntax=docker/dockerfile:1

# ============================================================================
# Stage 1 — Build
# Multi-module Maven build (domain -> application -> infrastructure -> boot).
# We build only the `boot` module plus the inner modules it depends on (-am).
# ============================================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy every module POM first so Docker can cache the dependency layer.
# If only source changes, this layer (and the download) is reused.
COPY pom.xml ./
COPY domain/pom.xml domain/
COPY application/pom.xml application/
COPY infrastructure/pom.xml infrastructure/
COPY boot/pom.xml boot/

# Warm the local Maven repo. Best-effort: a clean offline resolve can fail on
# multi-module reactors, so we don't let it break the build.
RUN mvn -B -ntp -pl boot -am dependency:go-offline || true

# Now the sources. Changing code only invalidates from here down.
COPY domain/src domain/src
COPY application/src application/src
COPY infrastructure/src infrastructure/src
COPY boot/src boot/src

# Build the executable Spring Boot jar (tests run in CI, not in the image).
RUN mvn -B -ntp -pl boot -am clean package -DskipTests

# ============================================================================
# Stage 2 — Runtime
# Slim JRE image, non-root user, container-aware heap sizing.
# ============================================================================
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as an unprivileged user — never run a public-facing app as root.
RUN groupadd --system crm2 && useradd --system --gid crm2 --no-create-home crm2

# The boot module produces the repackaged executable jar (the *.jar.original
# is the thin jar and is excluded by the glob).
COPY --from=build /build/boot/target/*.jar app.jar

USER crm2

# Matches `server.port` in application.yml.
EXPOSE 8080

# MaxRAMPercentage lets the JVM respect the container memory limit set in EasyPanel.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
