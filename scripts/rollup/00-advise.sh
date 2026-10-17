#!/usr/bin/env bash
# Advisory, read-only: scans every in-scope dataset at the bucket root and lists each one's
# yearly history/ folders in ascending order, flagging which are already archived vs. rollup
# candidates. Doesn't take <archive> <year> and doesn't use common.sh's lock/scratch/step-marker
# machinery, since it looks across all datasets rather than running one rollup.
# Usage: ./00-advise.sh
set -euo pipefail
ROLLUP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "$ROLLUP_DIR/scope.sh"

ROLLUP_ENV="$ROLLUP_DIR/rollup.env"
if [[ ! -f "$ROLLUP_ENV" ]]; then
	echo "🔴 missing $ROLLUP_ENV (copy rollup.env.sample to rollup.env and fill it in)" >&2
	exit 1
fi

set -a
# shellcheck disable=SC1090,SC1091
. "$ROLLUP_ENV"
set +a

for var in RCLONE_CONFIG_EVEREF_ACCOUNT RCLONE_CONFIG_EVEREF_KEY ROLLUP_BUCKET; do
	if [[ -z "${!var:-}" ]]; then
		echo "🔴 $var is not set in $ROLLUP_ENV" >&2
		exit 1
	fi
done

# Same fixed, account/key-only rclone setup as common.sh: the operator's own rclone.conf is
# ignored, and hard_delete stays false even though this script never deletes anything.
export RCLONE_CONFIG=/dev/null
export RCLONE_CONFIG_EVEREF_TYPE=b2
export RCLONE_CONFIG_EVEREF_HARD_DELETE=false
export TZ=UTC LC_ALL=C

REMOTE="everef:${ROLLUP_BUCKET}"
CURRENT_UTC_YEAR="$(TZ=UTC date +%Y)"

echo "## 🔵 Scanning bucket root"
ALL_ENTRIES="$(rclone lsf "$REMOTE/")"

for entry in $ALL_ENTRIES; do
	dataset="${entry%/}"
	if [[ "$entry" != */ ]] || ! in_rollup_scope "$dataset"; then
		continue
	fi

	HISTORY_LISTING="$(rclone lsf "$REMOTE/$dataset/history/" 2>/dev/null || true)"
	YEARS="$(grep -oE '^[0-9]{4}/$' <<<"$HISTORY_LISTING" | tr -d '/' | sort -n || true)"
	if [[ -z "$YEARS" ]]; then
		continue
	fi

	echo "## 🔵 $dataset"
	while IFS= read -r year; do
		if [[ "$year" -ge "$CURRENT_UTC_YEAR" ]]; then
			echo "  $year  current/unfinished"
			continue
		fi
		if grep -qx "${dataset}-${year}.tar.xz" <<<"$HISTORY_LISTING"; then
			echo "  $year  already archived"
		else
			echo "  $year  candidate"
		fi
	done <<<"$YEARS"
done

echo "✅ Done. Pick the oldest 'candidate' year for a dataset and run 01-preflight.sh <archive> <year>."
