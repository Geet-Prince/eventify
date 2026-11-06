# ── Build Stage ───────────────────────────────────────────────
# eclipse-temurin:21 is the current LTS; Java 27 is not yet released
# as a stable Temurin build (pom.xml targets java 27 preview — keep
# this in sync with your pom.xml <java.version> property).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw -B dependency:go-offline
COPY src/ src/
RUN ./mvnw -B clean package -DskipTests

# ── Runtime Stage ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]