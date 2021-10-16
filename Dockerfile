FROM adoptopenjdk:11-jdk-hotspot as builder
WORKDIR /application
# Set up gradle
COPY gradlew .
COPY gradle gradle
RUN chmod +x gradlew
RUN ./gradlew --version
# Copy source
COPY *.gradle.kts ./
COPY .git .git
COPY src src
# Build and extract layered jar
RUN ./gradlew --no-daemon bootJar && java -Djarmode=layertools -jar build/libs/*.jar extract

FROM adoptopenjdk:11-jre-hotspot
EXPOSE 8080
ENV server.port 8080
RUN adduser --system --group spring
USER spring:spring
WORKDIR /application
COPY --from=builder /application/dependencies .
COPY --from=builder /application/spring-boot-loader .
COPY --from=builder /application/snapshot-dependencies .
COPY --from=builder /application/application .
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
