#!/usr/bin/env bash
set -e

# This script scrapes the API at https://eve-incursions.de/ for archival purposes.
# It's a one-off script, not running regularly.
# The output is meant as a backfill for the existing incursions data at https://data.everef.net/incursions/

DIR="/tmp/eve-incursions-$(date +%s)"
echo "Outputting to: $DIR"


mkdir -p $DIR

PAGE=1
while [ true ]
do
  echo "Fetching page $PAGE"
  FILE="$DIR/$PAGE.json"
  QUERY="{\"query\":\"query{spawnLogs(page: $PAGE){items{id date state spawn {id establishedAt endedAt state constellation {id} influenceLogs{id date influence}}}}}\"}"
  curl -s -X POST -H 'content-type: application/json' https://eve-incursions.de/api --data "$QUERY" > $FILE
  ITEMS=$(cat $FILE | jq -r ".data.spawnLogs.items | length")
  if [ $ITEMS -eq 0 ]; then
    echo "No more items, exiting"
    rm $FILE
    break
  fi
  sleep 5
  PAGE=$((PAGE+1))
done

FINAL="$DIR/eve-incursions-de-$(date -u +%Y-%m-%d).json"
echo "Building output file: $FINAL"
ls -1 $DIR/*.json | xargs -I{} cat {} | \
  jq ".data.spawnLogs.items" | \
  jq -s "add" | \
  jq "sort_by(.id | tonumber)" > $FINAL

echo "Compressing"
xz $FINAL

echo "Complete"
