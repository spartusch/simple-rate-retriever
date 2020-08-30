#!/bin/bash

declare $(env -i `cat bin/configuration.vars`)
declare $(env -i `cat bin/secrets.vars`)

image=$project_group'/'$project_name':latest'

while getopts 'u:h:i:' argument
do
  case $argument in
    u) user=$OPTARG ;;
    h) host=$OPTARG ;;
    i) image=$OPTARG ;;
  esac
done

if [ -z $user ]; then echo "ERROR: No user provided. Please use parameter '-u {user}' to provide a ssh user."; exit 1; fi
if [ -z $host ]; then echo "ERROR: No host provided. Please use parameter '-h {host}' to provide a ssh host."; exit 1; fi

echo
echo "=> Sending docker image '$image' to ssh://$user@$host (might take a while) ..."
echo

rm_cmd="container_id=\$(docker ps -a --filter name=$project_name --format '{{.ID}}'); \
  if [ ! -z \$container_id ]; then docker stop \$container_id && docker container rm \$container_id; else echo No running container found; fi"

run_cmd="docker run -p$port:8080 \
  -e CMC_API_KEY=$cmc_api_key \
  -e spring.boot.admin.client.url=$admin_server \
  -d --network $project_group --name $project_name $image;"

echo "DEBUG: Command to remove existing container:"
echo $rm_cmd
echo

echo "DEBUG: Command to run shiny new container on port $port:"
echo $run_cmd
echo

docker save $image | gzip | ssh $user@$host "gunzip | docker load && $rm_cmd && $run_cmd"
echo
