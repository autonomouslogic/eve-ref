#!/usr/bin/env bash
# Step 3: download the remote year dir, verify against listing.json.
# Usage: ./20-download.sh <archive> <year>
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh" "$@"

DOWNLOAD_DIR="$SCRATCH/download/$YEAR_PATH"

echo "## 🔵 Download"
mkdir -p "$DOWNLOAD_DIR"
rclone copy --progress --bwlimit 10M --fast-list --transfers=8 --checkers=2 --max-backlog 1000000 \
  "$REMOTE/$YEAR_PATH" "$DOWNLOAD_DIR"
echo "rclone copy done"

echo "## 🔵 Checking against the remote"
if ! rclone check "$REMOTE/$YEAR_PATH" "$DOWNLOAD_DIR" --fast-list; then
	echo "🔴 rclone check found differences between the remote and $DOWNLOAD_DIR" >&2
	exit 1
fi
echo "rclone check: 0 differences"

echo "## 🔵 Checking file list, sizes and mtimes against the listing"
LOCAL_TSV="$(
	cd "$DOWNLOAD_DIR"
	while IFS= read -r -d '' f; do
		rel="${f#./}"
		size="$(stat -c%s "$f")"
		mtime="$(date -u -d "@$(stat -c%Y "$f")" +%Y-%m-%dT%H:%M:%SZ)"
		printf '%s\t%s\t%s\n' "$rel" "$size" "$mtime"
	done < <(find . -type f -print0)
)"
EXPECTED_TSV="$(jq -r '.[] | [.Path, (.Size | tostring), (.ModTime | sub("\\.[0-9]+Z$"; "Z"))] | @tsv' "$SCRATCH/listing.json")"

DIFF="$(diff <(sort <<<"$LOCAL_TSV") <(sort <<<"$EXPECTED_TSV") || true)"
if [[ -n "$DIFF" ]]; then
	echo "🔴 Local files differ from listing.json (path, size, mtime):" >&2
	echo "$DIFF" >&2
	exit 1
fi
LOCAL_COUNT="$(wc -l <<<"$LOCAL_TSV")"
echo "$LOCAL_COUNT files match listing.json exactly (path, size, mtime)"

echo "## 🔵 Checking bz2 integrity"
if ! find "$DOWNLOAD_DIR" -name '*.bz2' -print0 | xargs -0 -P"$(nproc)" -n1 bzip2 -t; then
	echo "🔴 bzip2 integrity check failed on one or more files" >&2
	exit 1
fi
echo "bzip2 -t passed on every .bz2 file"

DOWNLOADED_BYTES="$(jq '[.[].Size] | add // 0' "$SCRATCH/listing.json")"

mark_done \
	"files_downloaded=$LOCAL_COUNT" \
	"bytes_downloaded=$DOWNLOADED_BYTES"

echo "✅ Download OK for $ARCHIVE $YEAR"
