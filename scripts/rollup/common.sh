#!/usr/bin/env bash
# Shared setup for scripts/rollup/*.sh. Sourced, never run directly: `source common.sh "$@"`.
# See PLAN.md for the full design.

set -euo pipefail

ROLLUP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
STEP="$(basename "$0" .sh)"

STEP_ORDER=(00-preflight 10-list 20-download 30-prepare 40-archive 50-upload 60-delete 70-complete)

ROLLUP_SCOPE=(
	incursions
	skinr-listings
	skinr-details
	sovereignty-*
	industry-*
	system-jumps
	system-kills
	warzone*
	insurance-prices
	universe-structures
	faction-warfare-*
	military-campaigns
	markets-prices
	freelance-jobs
	structures
)

in_rollup_scope() {
	local archive="$1" pattern
	for pattern in "${ROLLUP_SCOPE[@]}"; do
		if [[ "$archive" == $pattern ]]; then
			return 0
		fi
	done
	return 1
}

if [[ $# -ne 2 ]]; then
	echo "🔴 usage: $(basename "$0") <archive> <year>" >&2
	exit 1
fi

ARCHIVE="$1"
YEAR="$2"

if [[ ! "$ARCHIVE" =~ ^[a-z0-9-]+$ ]]; then
	echo "🔴 invalid archive name: $ARCHIVE" >&2
	exit 1
fi

if ! in_rollup_scope "$ARCHIVE"; then
	echo "🔴 archive is not in the rollup scope: $ARCHIVE (see PLAN.md, \"Scope\")" >&2
	exit 1
fi

if [[ ! "$YEAR" =~ ^20[0-9]{2}$ ]]; then
	echo "🔴 invalid year: $YEAR" >&2
	exit 1
fi

CURRENT_UTC_YEAR="$(TZ=UTC date +%Y)"
if [[ "$YEAR" -ge "$CURRENT_UTC_YEAR" ]]; then
	echo "🔴 year must be a finished year, before the current UTC year ($CURRENT_UTC_YEAR): $YEAR" >&2
	exit 1
fi

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

# Disk-backed default; rollup.env can override for big datasets or a different disk layout.
ROLLUP_SCRATCH_ROOT="${ROLLUP_SCRATCH_ROOT:-/tmp/everef-rollup}"

# rclone is configured only from RCLONE_CONFIG_* env vars. This keeps the operator's own
# rclone.conf, and any remotes defined in it, out of the picture entirely. Only the account and
# key are secrets, so only those come from rollup.env; the rest of the "everef" remote is fixed
# here. hard_delete must never be true: deletes must stay soft, so the bucket lifecycle rules
# provide the undo window.
export RCLONE_CONFIG=/dev/null
export RCLONE_CONFIG_EVEREF_TYPE=b2
export RCLONE_CONFIG_EVEREF_HARD_DELETE=false

PUBLIC_BASE_URL="https://data.everef.net"
LOCK_FILE="/tmp/everef-rollup.lock"
REMOTE="everef:${ROLLUP_BUCKET}"
SCRATCH="${ROLLUP_SCRATCH_ROOT}/${ARCHIVE}-${YEAR}"
ARCHIVE_NAME="${ARCHIVE}-${YEAR}.tar.xz"
YEAR_PATH="${ARCHIVE}/history/${YEAR}"

exec 9>"$LOCK_FILE"
if ! flock -n 9; then
	echo "🔴 another rollup step is already running (lock: $LOCK_FILE)" >&2
	exit 1
fi

# Pin how tar/xz/bzip2 behave regardless of the operator's shell environment.
export TZ=UTC LC_ALL=C
export TAR_OPTIONS=
export XZ_OPT=
export XZ_DEFAULTS=
export BZIP=
export BZIP2=

mkdir -p "$SCRATCH/log"

if [[ -f "$SCRATCH/COMPLETE" ]]; then
	echo "🔴 rollup of $ARCHIVE $YEAR is already COMPLETE" >&2
	exit 1
fi

if [[ -f "$SCRATCH/$STEP.done" ]]; then
	echo "🔴 $STEP already completed, refusing to rerun" >&2
	exit 1
fi

for s in "${STEP_ORDER[@]}"; do
	if [[ "$s" == "$STEP" ]]; then
		break
	fi
	if [[ ! -f "$SCRATCH/$s.done" ]]; then
		echo "🔴 earlier step $s has not completed yet" >&2
		exit 1
	fi
done

LOG_FILE="$SCRATCH/log/${STEP}-$(date -u +%Y%m%dT%H%M%SZ).log"
exec > >(tee -a "$LOG_FILE") 2>&1

echo "# 🚀 $STEP: $ARCHIVE $YEAR"

# Call once all of a step's verifications have passed. Extra args are recorded as key facts
# alongside the UTC completion timestamp.
mark_done() {
	{
		date -u +%Y-%m-%dT%H:%M:%SZ
		printf '%s\n' "$@"
	} >"$SCRATCH/$STEP.done"
}
