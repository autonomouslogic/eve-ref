#!/usr/bin/env bash
# Step 3: download the remote year dir, verify against listing.json. Always loads from the remote; LOCAL_COPY_DIR, if
# set, only seeds the download dir first (see below).
# Usage: ./03-download.sh <archive> <year>
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh" "$@"

DOWNLOAD_DIR="$SCRATCH/download/$YEAR_PATH"

# Optional LOCAL_COPY_DIR: a local copy of the data site, laid out like the bucket. Used only as a faster way to
# get files we already have, and never trusted. Files there with the listed size and mtime are copied into the
# download dir first. The download below then still runs in full against the remote with --checksum, which
# compares SHA-1s instead of size and mtime and replaces any seeded file that differs. The checks after it run
# as usual. Without LOCAL_COPY_DIR, this does nothing.
CHECKSUM_FLAG=()
if [[ -n "${LOCAL_COPY_DIR:-}" ]]; then
	echo "🔵 Seeding from the local copy (unverified)"
	LOCAL_YEAR_DIR="$LOCAL_COPY_DIR/$YEAR_PATH"
	if [[ ! -d "$LOCAL_YEAR_DIR" ]]; then
		echo "$LOCAL_YEAR_DIR not found, downloading everything"
	else
		SEED_DIR="$SCRATCH/seed"
		rm -rf "$SEED_DIR"
		mkdir -p "$SEED_DIR" "$DOWNLOAD_DIR"
		jq -r '.[] | [.Path, (.Size | tostring), (.ModTime | sub("\\.[0-9]+Z$"; "Z") | fromdateiso8601 | tostring)] | @tsv' \
			"$SCRATCH/listing.json" | sort >"$SEED_DIR/expected.tsv"
		find "$LOCAL_YEAR_DIR" -type f -printf '%P\t%s\t%Ts\n' | sort >"$SEED_DIR/local.tsv"
		comm -12 "$SEED_DIR/expected.tsv" "$SEED_DIR/local.tsv" | cut -f1 >"$SEED_DIR/files-from.txt"
		SEED_COUNT="$(wc -l <"$SEED_DIR/files-from.txt")"
		echo "$SEED_COUNT of $(wc -l <"$SEED_DIR/expected.tsv") listed files in $LOCAL_YEAR_DIR have the listed size and mtime"
		if [[ "$SEED_COUNT" -gt 0 ]]; then
			rclone copy -v --transfers=8 --checkers=16 --files-from-raw "$SEED_DIR/files-from.txt" \
				"$LOCAL_YEAR_DIR" "$DOWNLOAD_DIR"
			echo "Seeded $SEED_COUNT files"
		fi
	fi
	# Also covers a rerun after an earlier seed: whatever is in the download dir may have come from the local copy.
	CHECKSUM_FLAG=(--checksum)
fi

echo "🔵 Download"
mkdir -p "$DOWNLOAD_DIR"
rclone copy -v ${CHECKSUM_FLAG[@]+"${CHECKSUM_FLAG[@]}"} --bwlimit 20M --fast-list --transfers=8 --checkers=2 --max-backlog 1000000 \
  "$REMOTE/$YEAR_PATH" "$DOWNLOAD_DIR"
echo "rclone copy done"

echo "🔵 Checking against the remote"
if ! rclone check "$REMOTE/$YEAR_PATH" "$DOWNLOAD_DIR" --fast-list; then
	echo "🔴 rclone check found differences between the remote and $DOWNLOAD_DIR" >&2
	exit 1
fi
echo "rclone check: 0 differences"

echo "🔵 Checking file list, sizes and mtimes against the listing"
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

echo "🔵 Checking bz2 integrity"
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
