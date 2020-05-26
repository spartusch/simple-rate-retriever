#!/bin/sh

declare $(env -i `cat bin/configuration.vars`)

./gradlew -PgenerateLaunchScript bootJar
./build/libs/simple-rate-retriever*.jar --spring.boot.admin.client.url=$admin_server
