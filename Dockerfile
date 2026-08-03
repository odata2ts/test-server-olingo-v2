# syntax=docker/dockerfile:1

# Runnable image of the "Library" OData V2 test server, Apache Olingo 2 implementation.
#
# Consumers start this image, point their client at http://<host>:<port>/odata/v2/library and get a
# server with fixed, well-known seed data.

FROM maven:3.9-eclipse-temurin-8 AS build
WORKDIR /src

# Resolve dependencies first, so a source change does not invalidate that layer.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B -DskipTests package

# Olingo 2 is a javax.* stack, so the runtime is a Java 8 JRE. The data lives in memory: nothing to
# migrate, nothing to seed, and every container starts from the identical, well-known state.
FROM eclipse-temurin:8-jre-alpine
WORKDIR /app
COPY --from=build /src/target/library-server.jar ./library-server.jar

EXPOSE 4004

HEALTHCHECK --interval=5s --timeout=3s --start-period=10s --retries=10 \
  CMD wget -q -O /dev/null http://127.0.0.1:4004/odata/v2/library/ || exit 1

ENTRYPOINT ["java", "-jar", "/app/library-server.jar"]
