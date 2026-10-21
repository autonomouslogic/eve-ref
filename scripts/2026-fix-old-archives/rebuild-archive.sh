#!/usr/bin/env bash
# Rebuilds one old pre-2023 <dataset>-<year>.tar.bz2 archive (from data.everef.net) into a
# .tar.xz archive matching the exact format scripts/rollup/05-archive.sh produces: GNU tar,
# --owner=0 --group=0 --numeric-owner --mode='a=r,u+w', LC_ALL=C sorted entries, xz -6.
#
# If a live history/<year>/ directory still exists on the remote for this dataset (the old
# "2022 archive built by mistake, raw data never deleted" problem), its files are merged in too
# — the old archives are disjoint from the live leftovers (verified by hand beforehand, see
# README.md), so any overlap found here is treated as an anomaly, not something to resolve
# automatically.
#
# This script only ever reads from data.everef.net and writes to its own scratch dir. It never
# uploads, deletes, or touches the remote. Output: <dataset>-<year>.tar.xz plus checksums, left
# in the scratch dir for manual review and manual upload.
#
# Usage: ./rebuild-archive.sh <dataset> <year>
set -euo pipefail

if [[ $# -ne 2 ]]; then
	echo "🔴 usage: $(basename "$0") <dataset> <year>" >&2
	exit 1
fi

DATASET="$1"
YEAR="$2"

if [[ ! "$DATASET" =~ ^[a-z0-9-]+$ ]]; then
	echo "🔴 invalid dataset name: $DATASET" >&2
	exit 1
fi

if [[ ! "$YEAR" =~ ^20[0-9]{2}$ ]]; then
	echo "🔴 invalid year: $YEAR" >&2
	exit 1
fi

for tool in curl jq bzip2 tar xz sha256sum sha1sum find sort flock stat date; do
	if ! command -v "$tool" >/dev/null 2>&1; then
		echo "🔴 Missing required tool: $tool" >&2
		exit 1
	fi
done
if ! tar --version | head -n1 | grep -q 'GNU tar'; then
	echo "🔴 Tar must be GNU tar" >&2
	exit 1
fi

export TZ=UTC LC_ALL=C
PUBLIC_BASE_URL="https://data.everef.net"
SCRATCH="/tmp/everef-fix-old-archives/${DATASET}-${YEAR}"
LOCK_FILE="/tmp/everef-fix-old-archives.lock"
OLD_ARCHIVE_NAME="${DATASET}-${YEAR}.tar.bz2"
NEW_ARCHIVE_NAME="${DATASET}-${YEAR}.tar.xz"
YEAR_PATH="${DATASET}/history/${YEAR}"

mkdir -p "$(dirname "$LOCK_FILE")"
exec 9>"$LOCK_FILE"
if ! flock -n 9; then
	echo "🔴 another rebuild-archive.sh run is already in progress (lock: $LOCK_FILE)" >&2
	exit 1
fi

if [[ -e "$SCRATCH" ]]; then
	echo "🔴 scratch dir already exists, remove it before retrying: $SCRATCH" >&2
	exit 1
fi
mkdir -p "$SCRATCH/log" "$SCRATCH/old" "$SCRATCH/stage"

LOG_FILE="$SCRATCH/log/rebuild-$(date -u +%Y%m%dT%H%M%SZ).log"
exec > >(tee -a "$LOG_FILE") 2>&1

echo "🚀 rebuild-archive: $DATASET $YEAR"

OLD_ARCHIVE_URL="$PUBLIC_BASE_URL/$DATASET/history/$OLD_ARCHIVE_NAME"
OLD_ARCHIVE_PATH="$SCRATCH/$OLD_ARCHIVE_NAME"

echo "🔵 Downloading old archive"
if ! curl -fsS -m 300 --retry 5 -o "$OLD_ARCHIVE_PATH" "$OLD_ARCHIVE_URL"; then
	echo "🔴 failed to download $OLD_ARCHIVE_URL" >&2
	exit 1
fi
echo "Downloaded $OLD_ARCHIVE_URL"

echo "🔵 Checking bzip2 integrity"
if ! bzip2 -t "$OLD_ARCHIVE_PATH"; then
	echo "🔴 bzip2 integrity check failed: $OLD_ARCHIVE_PATH" >&2
	exit 1
fi
echo "bzip2 -t passed"

echo "🔵 Checking old archive layout"
OLD_LISTING="$(tar -tjf "$OLD_ARCHIVE_PATH")"
TOP_DIR="${DATASET}-${YEAR}/"
if [[ "$(head -n1 <<<"$OLD_LISTING")" != "$TOP_DIR" ]]; then
	echo "🔴 expected first/only top-level entry to be $TOP_DIR" >&2
	exit 1
fi
BAD_ENTRIES="$(grep -vE "^${TOP_DIR}[^/]+\.json\$" <<<"$OLD_LISTING" | grep -vx "$TOP_DIR" || true)"
if [[ -n "$BAD_ENTRIES" ]]; then
	echo "🔴 unexpected entries in old archive (not $TOP_DIR<file>.json):" >&2
	echo "$BAD_ENTRIES" >&2
	exit 1
fi
OLD_FILE_COUNT="$(grep -cE "^${TOP_DIR}[^/]+\.json\$" <<<"$OLD_LISTING")"
if [[ "$OLD_FILE_COUNT" -eq 0 ]]; then
	echo "🔴 old archive has no data files" >&2
	exit 1
fi
echo "Old archive layout OK: $OLD_FILE_COUNT files under $TOP_DIR"

echo "🔵 Extracting old archive"
tar -xjf "$OLD_ARCHIVE_PATH" -C "$SCRATCH/old" --no-same-owner
echo "Extracted to $SCRATCH/old"

echo "🔵 Staging old archive files by date"
STAGE_YEAR_DIR="$SCRATCH/stage/$YEAR_PATH"
mkdir -p "$STAGE_YEAR_DIR"
STAGED_FROM_OLD=0
while IFS= read -r -d '' f; do
	name="$(basename "$f")"
	if [[ ! "$name" =~ _([0-9]{4}-[0-9]{2}-[0-9]{2})T[0-9]{2}:[0-9]{2}:[0-9]{2}\.json$ ]]; then
		echo "🔴 could not extract a date from filename: $name" >&2
		exit 1
	fi
	date="${BASH_REMATCH[1]}"
	if [[ "${date:0:4}" != "$YEAR" ]]; then
		echo "🔴 file date $date does not belong to year $YEAR: $name" >&2
		exit 1
	fi
	day_dir="$STAGE_YEAR_DIR/$date"
	mkdir -p "$day_dir"
	if [[ -e "$day_dir/$name" ]]; then
		echo "🔴 duplicate file would overwrite staged data: $day_dir/$name" >&2
		exit 1
	fi
	mv "$f" "$day_dir/$name"
	STAGED_FROM_OLD=$((STAGED_FROM_OLD + 1))
done < <(find "$SCRATCH/old/$TOP_DIR" -maxdepth 1 -type f -name '*.json' -print0)
if [[ "$STAGED_FROM_OLD" -ne "$OLD_FILE_COUNT" ]]; then
	echo "🔴 staged $STAGED_FROM_OLD files from old archive, expected $OLD_FILE_COUNT" >&2
	exit 1
fi
echo "Staged $STAGED_FROM_OLD files from the old archive"

echo "🔵 Checking for a live history/$YEAR/ leftover on the remote"
YEAR_INDEX_URL="$PUBLIC_BASE_URL/$DATASET/history/$YEAR/index.json"
YEAR_INDEX_STATUS="$(curl -s -o /dev/null -w '%{http_code}' "$YEAR_INDEX_URL")"
STAGED_FROM_LIVE=0
if [[ "$YEAR_INDEX_STATUS" == "404" ]]; then
	echo "No live history/$YEAR/ leftover (404) — nothing to merge"
elif [[ "$YEAR_INDEX_STATUS" != "200" ]]; then
	echo "🔴 unexpected HTTP $YEAR_INDEX_STATUS for $YEAR_INDEX_URL" >&2
	exit 1
else
	YEAR_INDEX_JSON="$(curl -fsS -m 60 --retry 5 "$YEAR_INDEX_URL")"
	DAYS="$(jq -r '(.directories // [])[].name' <<<"$YEAR_INDEX_JSON")"
	if [[ -z "$DAYS" ]]; then
		echo "🔴 live history/$YEAR/ exists but index.json lists no day directories" >&2
		exit 1
	fi
	echo "Live leftover found, merging days:"
	echo "$DAYS"
	while IFS= read -r day; do
		[[ -z "$day" ]] && continue
		day_dir="$STAGE_YEAR_DIR/$day"
		day_had_old_data=0
		[[ -d "$day_dir" ]] && day_had_old_data=1
		mkdir -p "$day_dir"
		DAY_INDEX_URL="$PUBLIC_BASE_URL/$DATASET/history/$YEAR/$day/index.json"
		DAY_INDEX_JSON="$(curl -fsS -m 60 --retry 5 "$DAY_INDEX_URL")"
		FILE_URLS="$(jq -r '(.files // [])[] | select(.name | endswith("index.html") | not) | .url' <<<"$DAY_INDEX_JSON")"
		if [[ -z "$FILE_URLS" ]]; then
			echo "🔴 no data files listed for $DATASET history/$YEAR/$day" >&2
			exit 1
		fi
		while IFS= read -r url; do
			[[ -z "$url" ]] && continue
			bz2_name="$(basename "$url")"
			json_name="${bz2_name%.bz2}"
			if [[ "$json_name" == "$bz2_name" ]]; then
				echo "🔴 expected a .bz2 file from the live listing, got: $bz2_name" >&2
				exit 1
			fi
			if [[ -e "$day_dir/$json_name" ]] && [[ "$day_had_old_data" -eq 1 ]]; then
				echo "🔴 live file collides with old-archive data (expected disjoint): $day_dir/$json_name" >&2
				exit 1
			fi
			bz2_path="$day_dir/$bz2_name"
			if ! curl -fsS -m 120 --retry 5 -o "$bz2_path" "$url"; then
				echo "🔴 failed to download $url" >&2
				exit 1
			fi
			if ! bzip2 -t "$bz2_path"; then
				echo "🔴 bzip2 integrity check failed: $bz2_path" >&2
				exit 1
			fi
			bunzip2 "$bz2_path"
			STAGED_FROM_LIVE=$((STAGED_FROM_LIVE + 1))
		done <<<"$FILE_URLS"
	done <<<"$DAYS"
	echo "Staged $STAGED_FROM_LIVE files from the live history/$YEAR/ leftover"
fi

STAGE_FILE_COUNT="$(find "$STAGE_YEAR_DIR" -type f | wc -l)"
EXPECTED_STAGE_COUNT=$((STAGED_FROM_OLD + STAGED_FROM_LIVE))
if [[ "$STAGE_FILE_COUNT" -ne "$EXPECTED_STAGE_COUNT" ]]; then
	echo "🔴 stage has $STAGE_FILE_COUNT files, expected $EXPECTED_STAGE_COUNT" >&2
	exit 1
fi

echo "🔵 Checking JSON validity"
if ! find "$STAGE_YEAR_DIR" -type f -print0 | xargs -0 -P"$(nproc)" -n500 bash -c \
	'out="$(jq -r input_filename "$@")" && diff <(printf "%s\n" "$out") <(printf "%s\n" "$@") >&2' bash; then
	echo "🔴 JSON check failed (diff above): invalid JSON, or files not holding exactly one JSON value" >&2
	exit 1
fi
echo "Every file holds exactly one valid JSON value"

echo "🔵 Hashing stage"
(
	cd "$SCRATCH/stage"
	find "$YEAR_PATH" -type f -exec sha256sum {} +
) | LC_ALL=C sort -k2 >"$SCRATCH/$DATASET-$YEAR.sha256"
echo "Wrote $DATASET-$YEAR.sha256 ($STAGE_FILE_COUNT files)"

NEW_ARCHIVE_PATH="$SCRATCH/$NEW_ARCHIVE_NAME"
NEW_ARCHIVE_TMP="$NEW_ARCHIVE_PATH.tmp"

echo "🔵 Building new archive"
(
	cd "$SCRATCH/stage"
	find "$YEAR_PATH" -type f -print0 \
		| LC_ALL=C sort -z \
		| tar --null --no-recursion -T - \
			--owner=0 --group=0 --numeric-owner --mode='a=r,u+w' \
			-cJf "$NEW_ARCHIVE_TMP"
)
mv "$NEW_ARCHIVE_TMP" "$NEW_ARCHIVE_PATH"
echo "Built $NEW_ARCHIVE_PATH"

echo "🔵 Checking xz integrity"
xz -t "$NEW_ARCHIVE_PATH"
echo "xz -t passed"

echo "🔵 Checking headers and order"
TAR_LISTING="$(tar -tvf "$NEW_ARCHIVE_PATH" --full-time)"

BAD_MODE="$(awk '$1 != "-rw-r--r--"' <<<"$TAR_LISTING")"
if [[ -n "$BAD_MODE" ]]; then
	echo "🔴 entries with unexpected mode (want -rw-r--r--):" >&2
	echo "$BAD_MODE" >&2
	exit 1
fi

BAD_OWNER="$(awk '$2 != "0/0"' <<<"$TAR_LISTING")"
if [[ -n "$BAD_OWNER" ]]; then
	echo "🔴 entries with unexpected owner (want 0/0):" >&2
	echo "$BAD_OWNER" >&2
	exit 1
fi

TAR_NAMES="$(awk '{print $6}' <<<"$TAR_LISTING")"
if ! LC_ALL=C sort -c <<<"$TAR_NAMES"; then
	echo "🔴 archive entries are not in LC_ALL=C sorted order" >&2
	exit 1
fi

TAR_COUNT="$(wc -l <<<"$TAR_LISTING")"
if [[ "$TAR_COUNT" -ne "$STAGE_FILE_COUNT" ]]; then
	echo "🔴 archive has $TAR_COUNT entries, stage has $STAGE_FILE_COUNT files" >&2
	exit 1
fi
echo "All $TAR_COUNT entries are regular files, mode -rw-r--r--, owner 0/0, sorted order"

echo "🔵 Checking contents hash-match the staged files"
tar -xJf "$NEW_ARCHIVE_PATH" \
	--to-command='printf "%s  %s\n" "$(sha256sum | cut -d" " -f1)" "$TAR_FILENAME"' \
	>"$SCRATCH/archive-contents.sha256"
CONTENTS_DIFF="$(diff "$SCRATCH/$DATASET-$YEAR.sha256" "$SCRATCH/archive-contents.sha256" || true)"
if [[ -n "$CONTENTS_DIFF" ]]; then
	echo "🔴 archive-contents.sha256 differs from $DATASET-$YEAR.sha256:" >&2
	echo "$CONTENTS_DIFF" >&2
	exit 1
fi
echo "archive-contents.sha256 matches $DATASET-$YEAR.sha256"

echo "🔵 Writing archive checksums"
sha256sum "$NEW_ARCHIVE_PATH" | awk -v n="$NEW_ARCHIVE_NAME" '{print $1"  "n}' >"$SCRATCH/archive.sha256"
sha1sum "$NEW_ARCHIVE_PATH" | awk -v n="$NEW_ARCHIVE_NAME" '{print $1"  "n}' >"$SCRATCH/archive.sha1"
echo "Wrote archive.sha256, archive.sha1"

echo "✅ rebuild-archive OK for $DATASET $YEAR"
echo "Result: $NEW_ARCHIVE_PATH"
echo "This script never uploads or touches the remote — review the archive and checksums in"
echo "$SCRATCH, then upload $NEW_ARCHIVE_NAME yourself."
