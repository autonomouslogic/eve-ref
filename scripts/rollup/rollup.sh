#!/usr/bin/env bash
# Runs every rollup step for one archive and year, in order, stopping at the first one that fails.
# Steps that already have a .done marker are skipped, so rerunning after a failure resumes where it stopped.
# Usage: ./rollup.sh <archive> <year>
set -euo pipefail

ROLLUP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [[ $# -ne 2 ]]; then
	echo "🔴 usage: $(basename "$0") <archive> <year>" >&2
	exit 1
fi
ARCHIVE="$1"
YEAR="$2"

# Same scratch dir as common.sh, which validates the arguments in every step. It's only used here to find the
# completion markers, so a wrong guess just means no step is skipped, and the step itself refuses to rerun.
if [[ -f "$ROLLUP_DIR/rollup.env" ]]; then
	# shellcheck disable=SC1091
	ROLLUP_SCRATCH_ROOT="$(. "$ROLLUP_DIR/rollup.env" >/dev/null 2>&1 && echo "${ROLLUP_SCRATCH_ROOT:-}")"
fi
SCRATCH="${ROLLUP_SCRATCH_ROOT:-/tmp/everef-rollup}/${ARCHIVE}-${YEAR}"

# The numbered step scripts, in order. advise.sh takes no arguments and isn't one of them.
STEPS=()
for script in "$ROLLUP_DIR"/[0-9][0-9]-*.sh; do
	STEPS+=("$script")
done

for script in "${STEPS[@]}"; do
	step="$(basename "$script" .sh)"
	if [[ -f "$SCRATCH/$step.done" ]]; then
		echo "⏭️  $step: already done, skipping"
		continue
	fi
	echo "▶️  $step"
	if ! "$script" "$ARCHIVE" "$YEAR"; then
		echo "🔴 $step failed: stopping. Fix it and rerun rollup.sh to resume" >&2
		exit 1
	fi
done

echo "🏁 All steps finished for $ARCHIVE $YEAR"
