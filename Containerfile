# Local testing image for Money Tracker (build/run with Podman on Windows).
# This is for verifying the app in a container before you ship — the actual
# AWS deployment runs the plain jar via systemd (see deploy/aws/README.md).
#
#   podman build -t money-tracker .
#   # quick smoke test (in-memory H2, no database needed):
#   podman run --rm -p 8099:8099 -e SPRING_PROFILES_ACTIVE=dev money-tracker
#   # then open http://localhost:8099/swagger-ui.html
#
# To test the prod profile against a real Postgres, see the notes at the bottom.

# ---- Stage 1: build the jar ------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Cache dependencies first so code-only changes don't re-download everything.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests \
 && cp "$(ls target/*.jar | grep -v plain)" app.jar

# ---- Stage 2: slim runtime -------------------------------------------------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
# Run as an unprivileged user.
RUN useradd --system --no-create-home --shell /usr/sbin/nologin appuser
COPY --from=build /app/app.jar app.jar
USER appuser
# Same heap cap as the EC2 service, so local behavior matches production.
ENV JAVA_OPTS="-Xms128m -Xmx384m -XX:+UseSerialGC"
EXPOSE 8080 8099
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

# ---- Testing the prod profile against Postgres -----------------------------
# Create a pod so the app can reach Postgres on localhost:
#   podman pod create --name mt -p 8080:8080
#   podman run -d --pod mt --name mt-db \
#     -e POSTGRES_DB=moneytracker -e POSTGRES_USER=moneytracker \
#     -e POSTGRES_PASSWORD=secret docker.io/library/postgres:15
#   podman run --rm --pod mt --name mt-app \
#     -e SPRING_PROFILES_ACTIVE=prod -e SERVER_PORT=8080 \
#     -e DB_URL=jdbc:postgresql://localhost:5432/moneytracker \
#     -e DB_USERNAME=moneytracker -e DB_PASSWORD=secret \
#     -e APP_JWT_SECRET=local-test-secret-at-least-32-bytes-long-xx \
#     -e SPRING_JPA_HIBERNATE_DDL_AUTO=update \
#     money-tracker
# Tear down:  podman pod rm -f mt
