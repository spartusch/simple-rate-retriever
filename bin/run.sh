#!/bin/sh

./gradlew -PgenerateLaunchScript bootJar
./build/libs/simple-rate-retriever*.jar --spring.boot.admin.client.url=http://localhost:18000
