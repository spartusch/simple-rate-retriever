#!/bin/bash

project_name='simple-rate-retriever'
project_group='com.github.spartusch'
project_port=18091

admin_server='http://admin-server:18000'

echo
echo "=> Starting to build and run '$project_group/$project_name' ..."
echo

container_id=$(docker ps --filter name=$project_name --format '{{.ID}}');
if [ -z $container_id ];
  then
    echo "=> Container '$project_name' is not running";
    echo
	else
	  echo "=> Stopping existing container '$project_name' ...";
	  docker stop $container_id > /dev/null;
	  echo
fi

echo "=> Building image '$project_group/$project_name' (might take a while) ..."

./gradlew bootBuildImage > /dev/null
echo

if [[ ! $(docker network ls | grep $project_group) ]];
  then
    echo "=> Creating network '$project_group' ..."
    docker network create $project_group > /dev/null
    echo
  else
    echo "=> Network '$project_group' found"
    echo
fi

echo "=> Starting new container '$project_name' on port $project_port ..."
docker run -p$project_port:$project_port \
    -e server.port=$project_port \
    -e spring.boot.admin.client.url=$admin_server \
    -d --rm --network $project_group --name $project_name \
    $project_group'/'$project_name':latest' > /dev/null

echo
echo "=> Cleaning up ...";
echo
docker system prune -f > /dev/null

echo "=> Done"
echo
