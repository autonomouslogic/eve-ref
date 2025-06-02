#!/bin/bash -e

# Runs a make command and calls a Discord webhook if there are any changes.
# Used by Github Actions.

COMMAND=$1
URL=$2

curl -fsS -m 10 --retry 5 -o /dev/null \
  -X POST -H "Content-Type: application/json" \
  --data "{\"content\": \"${COMMAND} test\"}" \
  $URL

make $COMMAND
if [[ `git status --porcelain` ]]; then
  curl -fsS -m 10 --retry 5 -o /dev/null \
    -X POST -H "Content-Type: application/json" \
    --data "{\"content\": \"${COMMAND} need to be updated\"}" \
    $URL
fi
