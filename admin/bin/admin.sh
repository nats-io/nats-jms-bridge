NATS_BRIDGE_HOME="${NATS_BRIDGE_HOME:-$PWD}"
NATS_ADMIN_HOST="${NATS_ADMIN_HOST:-http://localhost:8080}"

TOKEN=$(cat "$NATS_BRIDGE_HOME/config/admin.token")
COMMAND="$1"


case $COMMAND in

config)
  curl -s  -H "Authorization: Bearer $TOKEN" \
    "$NATS_ADMIN_HOST/api/v1/bridges/admin/config" | jq .
  ;;

running | bridge-running)
  curl -s  -H "Authorization: Bearer $TOKEN" \
    "$NATS_ADMIN_HOST/api/v1/control/bridges/running" | jq .
  ;;

started | bridge-started)
  curl -s -H "Authorization: Bearer $TOKEN" \
    "$NATS_ADMIN_HOST/api/v1/control/bridges/started" | jq .
  ;;

was-error | bridge-errors)
  curl -s -H "Authorization: Bearer $TOKEN" \
    "$NATS_ADMIN_HOST/api/v1/control/bridges/error/was-error" | jq .
  ;;

last-error | bridge-last-error)
  curl -s  -H "Authorization: Bearer $TOKEN" \
    "$NATS_ADMIN_HOST/api/v1/control/bridges/error/last" | jq .
  ;;

clear-error | bridge-clear-error)
  curl -s  -X POST -H "Authorization: Bearer $TOKEN" \
    "$NATS_ADMIN_HOST/api/v1/control/bridges/admin/clear/last/error"
  ;;

restart | bridge-restart)
  curl -s  -X POST -H "Authorization: Bearer $TOKEN" \
    "$NATS_ADMIN_HOST/api/v1/control/bridges/admin/restart" | jq .
  ;;

stop | bridge-stop)
  curl -s  -X POST -H "Authorization: Bearer $TOKEN" \
    "$NATS_ADMIN_HOST/api/v1/control/bridges/admin/stop" | jq .
  ;;

import)

  cat $2 | sed -e ':a' -e 'N' -e '$!ba' -e 's/\n/###___/g' >> "$2.temp.file"
  curl -s  -X PUT -H "Authorization: Bearer $TOKEN" \
     -H 'Content-Type: text/tsv' -d @"$2.temp.file"\
    "$NATS_ADMIN_HOST/api/v1/bridges/admin/config/import/bridges?name=$3&delim=$4" | jq .
  ;;

set-up-admin | admin)
  # shellcheck disable=SC2002
  ADMIN_JSON=$(cat config/initial-nats-bridge-logins.json | jq '.logins[] | select(.subject == "admin")')
  SUBJECT=$(echo "$ADMIN_JSON" | jq -r '.subject')
  SECRET=$(echo "$ADMIN_JSON" | jq -r '.secret')
  PUBLIC_KEY=$(echo "$ADMIN_JSON" | jq -r '.publicKey')

  JSON="{'subject':'$SUBJECT', 'publicKey' : '$PUBLIC_KEY', 'secret':'$SECRET' }"
  JSON=$(tr "'" '"' <<<"$JSON")

  TOKEN=$(curl -s  -X POST -d "$JSON" \
    -H "Content-Type: application/json" \
    "$NATS_ADMIN_HOST/api/v1/login/generateToken" | jq -r .token)
  echo "$TOKEN" >config/admin.token
  echo "$TOKEN"
  ;;

generatetoken | generateToken | generate-token | token | logins-generate-token)
  SUBJECT="$2"
  PUBLIC_KEY="$3"
  SECRET="$4"

  JSON="{'subject':'$SUBJECT', 'publicKey' : '$PUBLIC_KEY', 'secret':'$SECRET' }"
  JSON=$(tr "'" '"' <<<"$JSON")

  TOKEN=$(curl -s  -X POST -d "$JSON" \
    -H "Content-Type: application/json" \
    "$NATS_ADMIN_HOST/api/v1/login/generateToken | jq -r .token")
  echo "$TOKEN" >config/admin.token
  echo "$TOKEN"
  ;;

health)
  curl -s  "$NATS_ADMIN_HOST/manage/health" | jq .
  ;;

info)
  curl -s  "$NATS_ADMIN_HOST/manage/info"  | jq .
  ;;

kpi)
  curl -s  "$NATS_ADMIN_HOST/manage/prometheus"
  ;;

help)
cat << EndOfMessage

To use this tool you must install `jq`.
`jq` is a lightweight command-line JSON processor.
https://stedolan.github.io/jq/
(brew install jq or sudo apt-get install jq or https://stedolan.github.io/jq/download/)

## To set up admin tool for the first time from the NATS Bridge Admin directory run `set-up-admin`

  $ pwd
    /opt/synadia/nats-bridge/admin

  $ bin/admin.sh set-up-admin

## To check server health run `health`

  $ bin/admin.sh health

  {
    "status": "UP"
  }

## To see the server config run `config`

 $ bin/admin.sh config

  {
    "name": "Starter Config",
    "dateTime": "2020-04-24T20:38:50.574",
    "bridges": [
      {
        "name": "jmsToNatsSample",
        "bridgeType": "REQUEST_REPLY",
        "source": {
        ...

## To see if the bridge is running run `running`.
   $ bin/admin.sh running

    {
      "message": "Running?",
      "flag": true
    }

## To see if the bridge had any errors  run `was-error`.
  $ bin/admin.sh was-error

    {
      "message": "Errors?",
      "flag": false
    }

## To see the last error run `last-error`

   $ bin/admin.sh last-error
    {
      "message": "ERROR",
      "error": {
        "name": "JMSMessageBusException",
        "message": "Error receiving message",
        "root": "AMQ219017: Consumer is closed"
      }
    }

## To clear the last error use `clear-error`

  $ bin/admin.sh clear-error

## To resart the bridge use `restart`

  $ bin/admin.sh restart

## To Stop the bridge use `stop` (use restart to start it again)

  $ bin/admin.sh stop

## To genearte a token for a user use `generate-token`

  $ bin/admin.sh SUBJECT PUBLIC_KEY SECRET




EndOfMessage
  ;;

*)
  echo -n "use help $ admin help"
  ;;
esac
