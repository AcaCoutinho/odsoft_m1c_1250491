# Runtime image only: the jar is built by Maven (mvn verify) before docker build.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Non-root user; it owns /app because the app writes uploads to ./uploads-psoft-g1
RUN addgroup -S app && adduser -S app -G app && chown app:app /app
USER app
COPY target/*.jar app.jar
# Build commit, shown by /api/public/environment (set by the Jenkinsfile; after COPY so it does not invalidate the layers above)
ARG GIT_COMMIT=local
ENV APP_COMMIT=$GIT_COMMIT
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
