# syntax=docker/dockerfile:1.7

# ============================================================
# STAGE 1 - BUILD
# ============================================================

FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

# Copy Maven files first for better dependency caching.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN sed -i 's/\r$//' mvnw \
    && chmod +x mvnw

# Download Maven dependencies.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw --batch-mode --no-transfer-progress dependency:go-offline

COPY src/ src/

# Build the application JAR.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw --batch-mode --no-transfer-progress clean package -DskipTests \
    && JAR_FILE="$(find target -maxdepth 1 -type f -name '*.jar' \
       ! -name 'original-*.jar' \
       ! -name '*-sources.jar' \
       ! -name '*-javadoc.jar' | head -1)" \
    && test -n "$JAR_FILE" \
    && cp "$JAR_FILE" /app/app.jar


# ============================================================
# STAGE 2 - RUNTIME
# ============================================================

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Run the application as a non-root user.
RUN useradd --system --uid 10001 --no-create-home appuser

COPY --from=build --chown=10001:10001 /app/app.jar /app/app.jar

USER 10001

EXPOSE 8083

ENTRYPOINT ["java","-Duser.timezone=UTC","-XX:MaxRAMPercentage=75.0","-jar","/app/app.jar"]