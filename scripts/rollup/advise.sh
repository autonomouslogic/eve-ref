#!/usr/bin/env bash
# Advisory, read-only: scans every in-scope dataset and prints a ./rollup.sh <archive> <year>
# command for every rollup candidate (a finished history/<year> not yet archived), oldest year
# first. Doesn't take <archive> <year> and doesn't use common.sh's lock/scratch/step-marker
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

CANDIDATES=()

for dataset in "${ROLLUP_SCOPE[@]}"; do
	HISTORY_LISTING="$(http_history_listing "$dataset")"

	while IFS= read -r archive; do
		[[ -z "$archive" ]] && continue
		if [[ "$archive" != *.tar.xz ]]; then
			# The archive format (sorted paths, decompressed files, owner-free headers) is
			# only guaranteed from 05-archive.sh onward; anything else is a pre-2023 archive
			# made by hand or by an older process, not something rollup.sh itself would produce.
			echo "⚠️ $dataset: $archive is not a .tar.xz archive" >&2
		fi
	done <<<"$(grep -oE "^${dataset}-[0-9]{4}\.(tar\.xz|tar\.bz2)$" <<<"$HISTORY_LISTING" || true)"

	DIR_YEARS="$(grep -oE '^[0-9]{4}/$' <<<"$HISTORY_LISTING" | tr -d '/' | sort -nu || true)"
	if [[ -z "$DIR_YEARS" ]]; then
		continue
	fi

	while IFS= read -r year; do
		if [[ "$year" -ge "$CURRENT_UTC_YEAR" ]]; then
			continue
		fi
		if grep -qxE "${dataset}-${year}\.(tar\.xz|tar\.bz2)" <<<"$HISTORY_LISTING"; then
			# Both the year dir and its archive exist: 07-delete.sh should have hidden the dir
			# once 06-upload.sh succeeded, so something was left half-done.
			echo "⚠️ $dataset $year: history/$year/ still exists alongside its archive" >&2
		else
			CANDIDATES+=("$year $dataset")
		fi
	done <<<"$DIR_YEARS"
done

if [[ ${#CANDIDATES[@]} -eq 0 ]]; then
	echo "✅ Done, no candidates."
else
	while read -r year dataset; do
		echo "./scripts/rollup/rollup.sh $dataset $year &&"
	done <<<"$(printf '%s\n' "${CANDIDATES[@]}" | sort -n)"
	echo "✅ Done"

	if [[ -n "${DISCORD_URL:-}" ]]; then
		curl -fsS -m 10 --retry 5 -o /dev/null \
			-X POST -H "Content-Type: application/json" \
			--data '{"content": "Datasets ready for rollup. Run `./scripts/rollup/advise.sh`"}' \
			"$DISCORD_URL"
		echo "✉️ Discord notification sent"
	fi
fi
