FROM openjdk:11-jdk-slim
ADD gradle gradle
ADD gradlew .
RUN chmod +x gradlew && ./gradlew -version
ADD settings.gradle .
ADD build.gradle .
ADD .git .git
ADD src src
RUN ./gradlew --no-daemon build

FROM openjdk:11-jre-slim
ENV SERVER_PORT=18091
ENV ADMIN_SERVER=http://admin-server:18000
COPY --from=0 build/libs/*.jar .
EXPOSE $SERVER_PORT
ENTRYPOINT exec java $JAVA_OPTS -Dserver.port=$SERVER_PORT -Dspring.boot.admin.client.url=$ADMIN_SERVER -jar /*.jar
