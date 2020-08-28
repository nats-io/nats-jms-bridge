cd cicd
docker-compose -f compose/docker-compose-multibm.yml stop
docker-compose -f compose/docker-compose-multibm.yml rm
docker-compose -f compose/docker-compose-multibm.yml build
docker-compose -f compose/docker-compose-multibm.yml up
