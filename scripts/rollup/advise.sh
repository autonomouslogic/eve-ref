#!/usr/bin/env bash
# Advisory, read-only: scans every in-scope dataset and prints each one's yearly history/
# folders on two lines (rollup candidates, already archived), ascending and excluding the
# current year. Doesn't take <archive> <year> and doesn't use common.sh's lock/scratch/step-marker
# machinery, since it looks across all datasets rather than running one rollup.
#
# Reads each dataset's public history/index.json over plain HTTP (e.g.
# https://data.everef.net/system-kills/history/index.json), so it needs no credentials and no
# rollup.env — same as check-ready.sh.
# Usage: ./advise.sh
set -euo pipefail
ROLLUP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "$ROLLUP_DIR/lib/scope.sh"

echo "🚀 advise"

PUBLIC_BASE_URL="https://data.everef.net"
export TZ=UTC LC_ALL=C
CURRENT_UTC_YEAR="$(TZ=UTC date +%Y)"

# Fills HISTORY_LISTING with one entry per line: year directories as "YYYY/", archive files as
# their plain name.
http_history_listing() {
	local dataset="$1" index_json
	index_json="$(curl -fsS -m 10 --retry 5 "$PUBLIC_BASE_URL/$dataset/history/index.json" 2>/dev/null || true)"
	if [[ -z "$index_json" ]]; then
		return
	fi
	jq -r '((.directories // [])[].name | . + "/"), (.files // [])[].name' <<<"$index_json" 2>/dev/null || true
}

echo "🔵 Scanning $PUBLIC_BASE_URL"

OLDEST_YEAR=""
OLDEST_DATASET=""

for dataset in "${ROLLUP_SCOPE[@]}"; do
	HISTORY_LISTING="$(http_history_listing "$dataset")"
	# A rolled-up year's directory is gone, so years also come from existing archive files,
	# not just from year directories.
	YEARS="$(
		{
			grep -oE '^[0-9]{4}/$' <<<"$HISTORY_LISTING" | tr -d '/'
			grep -oE "^${dataset}-[0-9]{4}\.(tar\.xz|tar\.bz2)$" <<<"$HISTORY_LISTING" | grep -oE '[0-9]{4}'
		} | sort -nu || true
	)"
	if [[ -z "$YEARS" ]]; then
		continue
	fi

	candidates=()
	archived=()
	while IFS= read -r year; do
		if [[ "$year" -ge "$CURRENT_UTC_YEAR" ]]; then
			continue
		fi
		if grep -qxE "${dataset}-${year}\.(tar\.xz|tar\.bz2)" <<<"$HISTORY_LISTING"; then
			archived+=("$year")
		else
			candidates+=("$year")
		fi
	done <<<"$YEARS"

	echo "🔵 $dataset"
	echo "  candidates 👉 ${candidates[*]:-none}"
	echo "  archived ✅ ${archived[*]:-none}"

	if [[ ${#candidates[@]} -gt 0 ]] && { [[ -z "$OLDEST_YEAR" ]] || [[ "${candidates[0]}" -lt "$OLDEST_YEAR" ]]; }; then
		OLDEST_YEAR="${candidates[0]}"
		OLDEST_DATASET="$dataset"
	fi
done

if [[ -n "$OLDEST_YEAR" ]]; then
	echo "✅ Done"
	echo "Oldest candidate: $OLDEST_DATASET $OLDEST_YEAR"
	echo "Next command: ./rollup.sh $OLDEST_DATASET $OLDEST_YEAR"
else
	echo "✅ Done, no candidates."
fi
