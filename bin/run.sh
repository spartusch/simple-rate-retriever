#!/bin/sh

declare $(env -i `cat bin/secrets.vars`)

./gradlew -PgenerateLaunchScript bootJar
./build/libs/simple-rate-retriever*.jar --CMC_API_KEY=$cmc_api_key --ADMIN_USER=$admin_user --ADMIN_PASSWORD=$admin_password --spring.boot.admin.client.url=http://localhost:18000
