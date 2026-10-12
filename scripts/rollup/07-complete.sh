#!/usr/bin/env bash
# Step 8: recheck the archive and empty year dir, print a summary, write COMPLETE.
# Usage: ./07-complete.sh <archive> <year>
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh" "$@"

echo "## 🔵 Rechecking the remote archive"
ARCHIVE_SHA1="$(cut -d' ' -f1 "$SCRATCH/archive.sha1")"
ARCHIVE_SHA256="$(cut -d' ' -f1 "$SCRATCH/archive.sha256")"
ARCHIVE_BYTES="$(stat -c%s "$SCRATCH/$ARCHIVE_NAME")"
REMOTE_ARCHIVE_JSON="$(rclone lsjson --hash "$REMOTE/$ARCHIVE/history/" | jq --arg n "$ARCHIVE_NAME" '[.[] | select(.Name == $n)] | .[0] // null')"
if [[ "$REMOTE_ARCHIVE_JSON" == "null" ]]; then
	echo "🔴 $ARCHIVE_NAME is missing from the remote" >&2
	exit 1
fi
REMOTE_ARCHIVE_SHA1="$(jq -r '.Hashes.sha1 // ""' <<<"$REMOTE_ARCHIVE_JSON")"
if [[ "$REMOTE_ARCHIVE_SHA1" != "$ARCHIVE_SHA1" ]]; then
	echo "🔴 Remote archive SHA-1 $REMOTE_ARCHIVE_SHA1 does not match $ARCHIVE_SHA1" >&2
	exit 1
fi
echo "$ARCHIVE_NAME is present on the remote with unchanged SHA-1"

echo "## 🔵 Rechecking the year dir is still empty"
REMAINING="$(rclone lsf -R --fast-list "$REMOTE/$YEAR_PATH" 2>/dev/null || true)"
if [[ -n "$REMAINING" ]]; then
	echo "🔴 Files have reappeared under $REMOTE/$YEAR_PATH (likely a scheduled data-index run re-uploading" \
		"index files). Delete them and rerun:" >&2
	echo "$REMAINING" >&2
	exit 1
fi
echo "$REMOTE/$YEAR_PATH is still empty"

DATA_FILE_COUNT="$(jq '[.[] | select(.Path | test("index\\.html$") | not)] | length' "$SCRATCH/listing.json")"

echo "## 🔵 Summary"
echo "archive:          $ARCHIVE"
echo "year:             $YEAR"
echo "data file count:  $DATA_FILE_COUNT"
echo "archive path:     $ARCHIVE/history/$ARCHIVE_NAME"
echo "archive size:     $ARCHIVE_BYTES bytes"
echo "archive sha256:   $ARCHIVE_SHA256"
echo "archive sha1:     $ARCHIVE_SHA1"

date -u +%Y-%m-%dT%H:%M:%SZ >"$SCRATCH/COMPLETE"

mark_done \
	"data_file_count=$DATA_FILE_COUNT" \
	"archive_bytes=$ARCHIVE_BYTES" \
	"archive_sha256=$ARCHIVE_SHA256"

echo "✅ Rollup COMPLETE for $ARCHIVE $YEAR"
echo "Clear $SCRATCH once the 2-day lifecycle undo window has passed."
