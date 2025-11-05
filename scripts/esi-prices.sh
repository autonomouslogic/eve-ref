#!/bin/bash -e

ids=()
for id in "$@"; do
  body="$(curl -sS --fail https://ref-data.everef.net/blueprints/$id)"
  extracted="$(echo "$body" | jq '.activities[] | [ .materials, .products ][] | select(. != null) | .[].type_id')"
  for i in $extracted; do
    ids+=($i)
  done
done

query=()
for id in ${ids[*]}; do
  query+=(".type_id==$id")
done
joined=$(printf " or %s" "${query[@]}")
joined=${joined:4}

#curl -sS --fail https://esi.evetech.net/latest/markets/prices/ | jq "[.[] | select($joined)]"
cat ~/Downloads/markets-prices-2025-11-04_06-27-01.json | jq "[.[] | select($joined)]"
