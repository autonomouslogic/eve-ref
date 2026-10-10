#!/usr/bin/env bash
# Step 4: build stage/ from download/, drop index.html, decompress, hash-verify against the originals.
# Usage: ./03-prepare.sh <archive> <year>
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh" "$@"

DOWNLOAD_DIR="$SCRATCH/download/$YEAR_PATH"
STAGE_ROOT="$SCRATCH/stage"
STAGE_DIR="$STAGE_ROOT/$YEAR_PATH"

if [[ -e "$STAGE_ROOT" ]]; then
	echo "🔴 stage already exists, remove it before retrying: $STAGE_ROOT" >&2
	exit 1
fi

echo "## 🔵 Copying to stage"
mkdir -p "$(dirname "$STAGE_DIR")"
cp -a "$DOWNLOAD_DIR" "$STAGE_DIR"
echo "Copied $DOWNLOAD_DIR to $STAGE_DIR"

echo "## 🔵 Removing index.html"
TOTAL_COUNT="$(jq 'length' "$SCRATCH/listing.json")"
INDEX_HTML_COUNT="$(jq '[.[] | select(.Path | test("index\\.html$"))] | length' "$SCRATCH/listing.json")"
INDEX_JSON_COUNT="$(jq '[.[] | select(.Path | test("index\\.json$"))] | length' "$SCRATCH/listing.json")"

INDEX_HTML_DELETED=0
while IFS= read -r -d '' f; do
	rm -f "$f"
	INDEX_HTML_DELETED=$((INDEX_HTML_DELETED + 1))
done < <(find "$STAGE_DIR" -name 'index.html' -print0)

if [[ "$INDEX_HTML_DELETED" -ne "$INDEX_HTML_COUNT" ]]; then
	echo "🔴 Deleted $INDEX_HTML_DELETED index.html files, expected $INDEX_HTML_COUNT (from listing.json)" >&2
	exit 1
fi
echo "Deleted $INDEX_HTML_DELETED index.html files"

echo "## 🔵 Decompressing"
find "$STAGE_DIR" -name '*.bz2' -print0 | xargs -0 -P"$(nproc)" -n500 bunzip2
echo "Decompressed all .bz2 files"

echo "## 🔵 Checking file count"
STAGE_FILE_COUNT="$(find "$STAGE_DIR" -type f | wc -l)"
EXPECTED_STAGE_COUNT=$((TOTAL_COUNT - INDEX_HTML_COUNT))
if [[ "$STAGE_FILE_COUNT" -ne "$EXPECTED_STAGE_COUNT" ]]; then
	echo "🔴 Stage has $STAGE_FILE_COUNT files, expected $EXPECTED_STAGE_COUNT (data + index.json from listing.json)" >&2
	exit 1
fi
echo "Stage has $STAGE_FILE_COUNT files ($EXPECTED_STAGE_COUNT expected: data + index.json)"

echo "## 🔵 Hashing"
(
	cd "$STAGE_ROOT"
	find "$YEAR_PATH" -type f -exec sha256sum {} +
) | LC_ALL=C sort -k2 >"$SCRATCH/$ARCHIVE-$YEAR.sha256"
echo "Wrote $ARCHIVE-$YEAR.sha256 ($STAGE_FILE_COUNT files)"

echo "## 🔵 Checking mtimes"
STAGE_MTIME_TSV="$(
	cd "$STAGE_DIR"
	while IFS= read -r -d '' f; do
		rel="${f#./}"
		mtime="$(date -u -d "@$(stat -c%Y "$f")" +%Y-%m-%dT%H:%M:%SZ)"
		printf '%s\t%s\n' "$rel" "$mtime"
	done < <(find . -type f -print0)
)"
EXPECTED_MTIME_TSV="$(jq -r '
	.[]
	| select(.Path | test("index\\.html$") | not)
	| [(.Path | sub("\\.bz2$"; "")), (.ModTime | sub("\\.[0-9]+Z$"; "Z"))]
	| @tsv
	' "$SCRATCH/listing.json")"

MTIME_DIFF="$(diff <(sort <<<"$STAGE_MTIME_TSV") <(sort <<<"$EXPECTED_MTIME_TSV") || true)"
if [[ -n "$MTIME_DIFF" ]]; then
	echo "🔴 Stage mtimes differ from listing.json (path, mtime):" >&2
	echo "$MTIME_DIFF" >&2
	exit 1
fi
echo "mtimes match listing.json for all $STAGE_FILE_COUNT files"

echo "## 🔵 Checking JSON validity"
if ! find "$STAGE_DIR" -type f -print0 | xargs -0 -P"$(nproc)" -n1 jq empty; then
	echo "🔴 jq empty failed on one or more files in stage" >&2
	exit 1
fi
echo "jq empty passed on every file"

STAGE_BYTES="$(du -sb "$STAGE_DIR" | cut -f1)"

mark_done \
	"files_prepared=$STAGE_FILE_COUNT" \
	"index_html_deleted=$INDEX_HTML_DELETED" \
	"index_json_count=$INDEX_JSON_COUNT" \
	"stage_bytes=$STAGE_BYTES"

echo "✅ Prepare OK for $ARCHIVE $YEAR"
