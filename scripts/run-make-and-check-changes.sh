#!/bin/bash -e

# Runs a make command and calls a Discord webhook if there are any changes.
# Used by Github Actions.

COMMAND=$1
URL=$2

$MAKE="make $COMMAND"
echo Running $MAKE
$MAKE
if [[ `git status --porcelain` ]]; then
  echo Changes detected
  git status
  curl -fsS -m 10 --retry 5 -o /dev/null \
    -X POST -H "Content-Type: application/json" \
    --data "{\"content\": \"${COMMAND} need to be updated\"}" \
    $URL
else
  echo No changes detected
fi
