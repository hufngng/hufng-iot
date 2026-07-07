# Build context is the repo root (see infra/docker-compose.yml) so this stage
# can see hufng-common, which hufng-report depends on via the reactor.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -pl hufng-report -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/hufng-report/target/hufng-report-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
