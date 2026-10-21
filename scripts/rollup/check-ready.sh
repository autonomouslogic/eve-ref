#!/usr/bin/env bash
# Advisory, read-only: stops at the first in-scope dataset with a rollup candidate (a finished
# history/<year> not yet archived) and pings DATA_CHECKS_HEALTHCHECK_URL. Reads the public
# history/index.json for each dataset (e.g. https://data.everef.net/system-kills/history/index.json)
# instead of listing the bucket, so it needs no credentials. Doesn't walk every dataset — exits
# as soon as one candidate is found.
# Exits non-zero when a candidate was found, so a caller (e.g. a GitHub Actions step) can chain
# a `|| ...` notification off it. Still runs the check if DATA_CHECKS_HEALTHCHECK_URL isn't set,
# it just skips pinging it.
# Usage: ./check-ready.sh
set -euo pipefail
ROLLUP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "$ROLLUP_DIR/lib/scope.sh"

echo "🚀 check-ready"

export TZ=UTC LC_ALL=C
PUBLIC_BASE_URL="https://data.everef.net"
CURRENT_UTC_YEAR="$(TZ=UTC date +%Y)"

for dataset in "${ROLLUP_SCOPE[@]}"; do
	INDEX_URL="$PUBLIC_BASE_URL/$dataset/history/index.json"
	INDEX_JSON="$(curl -fsS -m 10 --retry 5 "$INDEX_URL" 2>/dev/null || true)"
	if [[ -z "$INDEX_JSON" ]]; then
		continue
	fi

	YEARS="$(jq -r '(.directories // [])[].name | select(test("^[0-9]{4}$"))' <<<"$INDEX_JSON" | sort -n)"
	if [[ -z "$YEARS" ]]; then
		continue
	fi

	while IFS= read -r year; do
		if [[ "$year" -ge "$CURRENT_UTC_YEAR" ]]; then
			continue
		fi
		if ! jq -e --arg xz "${dataset}-${year}.tar.xz" --arg bz2 "${dataset}-${year}.tar.bz2" \
			'(.files // [])[] | select(.name == $xz or .name == $bz2)' <<<"$INDEX_JSON" >/dev/null; then
			echo "🔵 Ready: $dataset $year"
			if [[ -n "${DATA_CHECKS_HEALTHCHECK_URL:-}" ]]; then
				echo "🔵 Pinging healthcheck"
				curl -fsS -m 10 --retry 5 -o /dev/null "$DATA_CHECKS_HEALTHCHECK_URL"
			fi
			echo "✅ Done, found a candidate: $dataset $year"
			exit 1
		fi
	done <<<"$YEARS"
done

echo "✅ Done, no candidates."
