# Runtime image only: the jar is built by Maven (mvn verify) before docker build.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Non-root user; it owns /app because the app writes uploads to ./uploads-psoft-g1
RUN addgroup -S app && adduser -S app -G app && chown app:app /app
USER app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
