cd cicd
docker-compose -f compose/docker-compose-nats-tls-multbridge.yml stop
docker-compose -f compose/docker-compose-nats-tls-multbridge.yml rm
docker-compose -f compose/docker-compose-nats-tls-multbridge.yml build
docker-compose -f compose/docker-compose-nats-tls-multbridge.yml up
