#!/usr/bin/env bash
set -e

# One-off script for fixing the timestamps on the Fuzzwork orderset files.
# Kept mainly for future reference.

DIR="$1"
echo "Processing directory: $DIR"
sleep 5

find $DIR -type f -name "fuzzwork-orderset-*.csv.gz" | {
  while read FILE; do
    echo File: $FILE
    BASE="$(basename $FILE)"
    TIME="$(echo $BASE | egrep -oe '[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}-[0-9]{2}')"
    TIME="$(echo $TIME | sed 's/_/ /')"
    TIME="$(echo $TIME | sed 's/-/:/3')"
    TIME="$(echo $TIME | sed 's/-/:/3')"
    touch -d "$TIME UTC" $FILE
  done
}
