#!/usr/bin/env bash
# Step 7: dry-run preview, rclone delete, verify the year dir is gone.
# Usage: ./07-delete.sh <archive> <year>
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh" "$@"

if [[ ! "$YEAR_PATH" =~ ^[a-z0-9-]+/history/20[0-9]{2}$ ]]; then
	echo "🔴 refusing to delete, year path doesn't look like <archive>/history/<year>: $YEAR_PATH" >&2
	exit 1
fi

TOTAL_COUNT="$(jq 'length' "$SCRATCH/listing.json")"

# Prints the files left under the year dir, one path per line. rclone exits 3 (directory not
# found) once nothing is left, which counts as empty. Any other failure aborts.
list_remaining() {
	local status=0
	rclone lsf -R --files-only --fast-list "$REMOTE/$YEAR_PATH" || status=$?
	if [[ "$status" -ne 0 && "$status" -ne 3 ]]; then
		echo "🔴 rclone lsf of $REMOTE/$YEAR_PATH failed with exit code $status" >&2
		return 1
	fi
}

# A rerun after a partial delete, or after a later check failed, finds fewer files than
# listing.json. That's fine, as long as every remaining file is one from listing.json.
echo "🔵 Checking what's left against the listing"
REMAINING="$(list_remaining)"
REMAINING_COUNT="$(grep -c . <<<"$REMAINING" || true)"
UNKNOWN="$(comm -23 <(sort <<<"$REMAINING") <(jq -r '.[].Path' "$SCRATCH/listing.json" | sort))"
if [[ -n "$UNKNOWN" ]]; then
	echo "🔴 Files under $REMOTE/$YEAR_PATH that are not in listing.json:" >&2
	echo "$UNKNOWN" >&2
	exit 1
fi
echo "$REMAINING_COUNT of $TOTAL_COUNT files from listing.json are left, no unknown files"

if [[ "$REMAINING_COUNT" -eq 0 ]]; then
	echo "Nothing left to delete (an earlier run already deleted everything), going straight to the checks"
else
	echo "🔵 Dry-run preview"
	DRY_RUN_OUTPUT="$(rclone delete --dry-run --fast-list "$REMOTE/$YEAR_PATH" 2>&1)" || true
	echo "$DRY_RUN_OUTPUT"
	DRY_RUN_COUNT="$(grep -c 'Skipped delete' <<<"$DRY_RUN_OUTPUT" || true)"
	if [[ "$DRY_RUN_COUNT" -ne "$REMAINING_COUNT" ]]; then
		echo "🔴 Dry-run would delete $DRY_RUN_COUNT files, $REMAINING_COUNT are left" >&2
		exit 1
	fi
	echo "Dry-run would delete $DRY_RUN_COUNT files, matches what's left"

	echo "🔵 Deleting"
	echo "Hiding $REMAINING_COUNT files under $REMOTE/$YEAR_PATH"
	echo "(soft delete: hidden versions are kept for 2 days by the bucket lifecycle rules)"
	rclone delete -v --fast-list --max-delete "$REMAINING_COUNT" "$REMOTE/$YEAR_PATH"
	echo "rclone delete done"
fi

echo "🔵 Checking the year dir is empty"
REMAINING="$(list_remaining)"
if [[ -n "$REMAINING" ]]; then
	echo "🔴 Files remain under $REMOTE/$YEAR_PATH:" >&2
	echo "$REMAINING" >&2
	exit 1
fi
echo "$REMOTE/$YEAR_PATH is empty"

SAMPLE_SIZE=10
echo "🔵 Checking $SAMPLE_SIZE random old public URLs are gone"
SAMPLE_PATHS="$(jq -r '.[].Path' "$SCRATCH/listing.json" | shuf -n "$SAMPLE_SIZE")"
while IFS= read -r path; do
	url="$PUBLIC_BASE_URL/$ARCHIVE/history/$YEAR/$path"
	status=""
	for attempt in 1 2 3; do
		status="$(curl -s -o /dev/null -w '%{http_code}' "$url")"
		if [[ "$status" == "404" ]]; then
			break
		fi
		echo "Not 404 yet (attempt $attempt): $url -> $status"
		sleep 5
	done
	if [[ "$status" != "404" ]]; then
		echo "🔴 Expected 404 for $url, got $status" >&2
		exit 1
	fi
	echo "404 confirmed: $url"
done <<<"$SAMPLE_PATHS"

echo "🔵 Checking the archive is still present, unchanged"
ARCHIVE_SHA1="$(cut -d' ' -f1 "$SCRATCH/archive.sha1")"
REMOTE_ARCHIVE_JSON="$(rclone lsjson --hash "$REMOTE/$ARCHIVE/history/" | jq --arg n "$ARCHIVE_NAME" '[.[] | select(.Name == $n)] | .[0] // null')"
if [[ "$REMOTE_ARCHIVE_JSON" == "null" ]]; then
	echo "🔴 $ARCHIVE_NAME is missing from the remote after delete" >&2
	exit 1
fi
REMOTE_ARCHIVE_SHA1="$(jq -r '.Hashes.sha1 // ""' <<<"$REMOTE_ARCHIVE_JSON")"
if [[ "$REMOTE_ARCHIVE_SHA1" != "$ARCHIVE_SHA1" ]]; then
	echo "🔴 Remote archive SHA-1 $REMOTE_ARCHIVE_SHA1 does not match $ARCHIVE_SHA1" >&2
	exit 1
fi
echo "$ARCHIVE_NAME is still present with unchanged SHA-1"

mark_done \
	"deleted_count=$TOTAL_COUNT" \
	"deleted_this_run=$REMAINING_COUNT" \
	"remote_archive_sha1=$REMOTE_ARCHIVE_SHA1"

echo "✅ Delete OK for $ARCHIVE $YEAR"
