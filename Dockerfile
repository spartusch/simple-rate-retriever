FROM openjdk:11-jre-slim as builder
WORKDIR application
COPY build/libs/*.jar application/
RUN java -Djarmode=layertools -jar application/*.jar extract
RUN rm application/*.jar

FROM openjdk:11-jre-slim
EXPOSE 8080
ENV server.port 8080
WORKDIR application
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
