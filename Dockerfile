FROM alpine:latest as builder
# Install JDK
RUN apk add --no-cache openjdk17-jdk
# Set up project
WORKDIR /application
# Set up gradle
COPY gradlew .
COPY gradle gradle
RUN chmod +x gradlew
RUN ./gradlew --version
# Copy source
COPY *.gradle.kts .
COPY gradle.lockfile .
COPY .git .git
COPY src src
# Build and extract layered jar
RUN ./gradlew --no-daemon bootJar && java -Djarmode=layertools -jar build/libs/*.jar extract

FROM alpine:latest
EXPOSE 8080
ENV server.port 8080
RUN apk add --no-cache openjdk17-jre-headless
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
WORKDIR /application
COPY --from=builder /application/dependencies .
COPY --from=builder /application/spring-boot-loader .
COPY --from=builder /application/snapshot-dependencies .
COPY --from=builder /application/application .
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
