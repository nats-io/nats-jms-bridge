docker ps  | grep cloudurable | awk '{print $1}' | xargs docker stop
docker ps -a | grep cloudurable | awk '{print $1}' | xargs docker rm
docker images | grep cloudurable | awk '{print $3}' | xargs docker  rmi
