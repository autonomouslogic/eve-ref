#!/bin/bash -e

URL=$1

make verify-ref-data-models || \
  curl -fsS -m 10 --retry 5 -o /dev/null \
    -X POST -H "Content-Type: application/json" \
    --data "{\"content\": \"Reference data models need to be updated\"}" \
    $URL
