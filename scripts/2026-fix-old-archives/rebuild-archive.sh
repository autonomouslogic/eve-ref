#!/usr/bin/env bash
# Rebuilds one old pre-2023 <dataset>-<year>.tar.bz2 archive (from data.everef.net) into a
# .tar.xz archive matching the exact format scripts/rollup/05-archive.sh produces: GNU tar,
# --owner=0 --group=0 --numeric-owner --mode='a=r,u+w', LC_ALL=C sorted entries, xz -6. Old
# archive files are also renamed from their old internal scheme (e.g.
# sovereigntycampaigns_2019-01-01T00:20:09.json) to the current convention
# (<dataset>-<date>_<HH-MM-SS>.json) to match live/rollup naming. Old archive files that are just a
# saved HTTP error response (known bodies only, see ERROR_PAGE_TITLES/ERROR_JSON_BODIES) are left out
# of the new archive and listed in removed.tsv.
#
# If a live history/<year>/ directory still exists on the remote for this dataset (the old
# "2022 archive built by mistake, raw data never deleted" problem), its files are merged in too
# — the old archives are disjoint from the live leftovers (verified beforehand, see README.md),
# so every live file must come after the last old-archive file, and any overlap aborts rather
# than being resolved automatically. The live listing is validated strictly, every download is
# checked against the size and MD5 data-index recorded for it, and every live file that went
# into the archive is recorded (live-files.tsv, live-files.txt) so clearing history/<year>/
# afterwards can be limited to exactly those files.
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

for tool in curl jq bzip2 tar xz sha256sum sha1sum md5sum find sort comm diff flock stat date; do
	if ! command -v "$tool" >/dev/null 2>&1; then
		echo "🔴 Missing required tool: $tool" >&2
		exit 1
	fi
done
if ! tar --version | head -n1 | grep -q 'GNU tar'; then
	echo "🔴 Tar must be GNU tar" >&2
	exit 1
fi

# Pin how tar/xz/bzip2 behave regardless of the operator's shell environment.
export TZ=UTC LC_ALL=C
export TAR_OPTIONS=
export XZ_OPT=
export XZ_DEFAULTS=
export BZIP=
export BZIP2=

PUBLIC_BASE_URL="https://data.everef.net"
SCRATCH="/tmp/everef-fix-old-archives/${DATASET}-${YEAR}"
LOCK_FILE="/tmp/everef-fix-old-archives.lock"
OLD_ARCHIVE_NAME="${DATASET}-${YEAR}.tar.bz2"
NEW_ARCHIVE_NAME="${DATASET}-${YEAR}.tar.xz"
YEAR_PATH="${DATASET}/history/${YEAR}"
YEAR_INDEX_URL="$PUBLIC_BASE_URL/$YEAR_PATH/index.json"
LIVE_DIR="$SCRATCH/live"
LIVE_MANIFEST="$SCRATCH/live-files.tsv"

fetch_index() {
	local url="$1" dest="$2"
	mkdir -p "$(dirname "$dest")"
	if ! curl -fsS -m 60 --retry 5 -o "$dest" "$url"; then
		echo "🔴 failed to download $url" >&2
		exit 1
	fi
}

# Fetches the index.json of every day in $DAYS into $1/<day>/index.json.
fetch_day_indexes() {
	local dest="$1" day
	while IFS= read -r day; do
		fetch_index "$PUBLIC_BASE_URL/$YEAR_PATH/$day/index.json" "$dest/$day/index.json"
	done <<<"$DAYS"
}

# One line per index.json under $1: its relative path, then its content with keys and entries
# sorted, so two fetches of an unchanged listing compare equal however data-index orders them.
normalize_listing() {
	local dir="$1" f
	while IFS= read -r f; do
		printf '%s\t%s\n' "${f#"$dir"/}" "$(jq -S -c '{path, files: ((.files // []) | sort_by(.name)), directories: ((.directories // []) | sort_by(.name))}' "$f")"
	done < <(find "$dir" -name index.json | LC_ALL=C sort)
}

# Checks a downloaded file against the size and etag data-index recorded for it. The etag is the
# object's MD5 (checked against real downloads, including archives uploaded in parts over 200 MiB).
verify_download() {
	local path="$1" want_size="$2" want_md5="$3" got_size got_md5
	got_size="$(stat -c%s "$path")"
	if [[ "$got_size" != "$want_size" ]]; then
		echo "🔴 size mismatch for $path: got $got_size, index says $want_size" >&2
		exit 1
	fi
	got_md5="$(md5sum "$path" | cut -d' ' -f1)"
	if [[ "$got_md5" != "$want_md5" ]]; then
		echo "🔴 MD5 mismatch for $path: got $got_md5, index etag says $want_md5" >&2
		exit 1
	fi
}

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

echo "🔵 Checking old archive against the history index"
fetch_index "$PUBLIC_BASE_URL/$DATASET/history/index.json" "$SCRATCH/history-index.json"
OLD_ARCHIVE_ROW="$(jq -r --arg n "$OLD_ARCHIVE_NAME" --arg u "$OLD_ARCHIVE_URL" '
	[(.files // [])[] | select(.name == $n)]
	| if length == 1 and .[0].url == $u then (.[0] | [.size, .etag] | map(tostring) | @tsv) else empty end
	' "$SCRATCH/history-index.json")"
if [[ -z "$OLD_ARCHIVE_ROW" ]]; then
	echo "🔴 history/index.json does not list exactly one $OLD_ARCHIVE_NAME at $OLD_ARCHIVE_URL" >&2
	exit 1
fi
IFS=$'\t' read -r OLD_ARCHIVE_SIZE OLD_ARCHIVE_ETAG <<<"$OLD_ARCHIVE_ROW"
if [[ ! "$OLD_ARCHIVE_SIZE" =~ ^[1-9][0-9]*$ || ! "$OLD_ARCHIVE_ETAG" =~ ^[0-9a-f]{32}$ ]]; then
	echo "🔴 missing or bad size/etag for $OLD_ARCHIVE_NAME in history/index.json: size=$OLD_ARCHIVE_SIZE etag=$OLD_ARCHIVE_ETAG" >&2
	exit 1
fi
verify_download "$OLD_ARCHIVE_PATH" "$OLD_ARCHIVE_SIZE" "$OLD_ARCHIVE_ETAG"
echo "Old archive matches history/index.json (size $OLD_ARCHIVE_SIZE, MD5 $OLD_ARCHIVE_ETAG)"

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

echo "🔵 Removing saved error responses"
# The old scraper sometimes saved an HTTP error response in place of a snapshot. Only files whose
# whole body is one of these known responses are removed (moved to removed/ for review). Any other
# HTML file, or any other {"error": ...} object, aborts the run so it can be reviewed first.
ERROR_PAGE_TITLES=("502 Bad Gateway" "504 Gateway Time-out")
ERROR_JSON_BODIES=(
	'{"error":"Not found"}'
	'{"error":"No reply within 10 seconds"}'
	'{"error":"Timeout contacting tranquility","timeout":10}'
	'{"error":"The datasource tranquility is temporarily unavailable"}'
	'{"error":"timeout contacting endpoint"}'
	"{\"error\":\"This software has exceeded the error limit for ESI. If you are a user, please contact the maintainer of this software. If you are a developer/maintainer, please make a greater effort in the future to receive valid responses. For tips on how, come have a chat with us in #esi on tweetfleet slack. If you're not on tweetfleet slack yet, you can get an invite here -> https://www.fuzzwork.co.uk/tweetfleet-slack-invites/\"}"
	"{\"error\":\"You have been banned from using ESI. Please come chat with us in #esi on tweetfleet slack and we can work to avoid this situation in the future. If you're not on tweetfleet slack yet, you can get an invite here -> https://www.fuzzwork.co.uk/tweetfleet-slack-invites/\"}"
)
REMOVED_DIR="$SCRATCH/removed"
REMOVED_LIST="$SCRATCH/removed.tsv"
mkdir -p "$REMOVED_DIR"
: >"$REMOVED_LIST"
REMOVED_FROM_OLD=0
UNKNOWN_ERRORS=()
while IFS= read -r -d '' f; do
	first=""
	IFS= read -r -n1 first <"$f" || true
	if [[ "$first" != "<" && "$first" != "{" ]]; then
		continue
	fi
	name="$(basename "$f")"
	body="$(<"$f")"
	reason=""
	if [[ "$first" == "<" ]]; then
		for title in "${ERROR_PAGE_TITLES[@]}"; do
			if [[ "$body" == "<html>"* && "$body" == *"<title>$title</title>"* && "$body" == *"<h1>$title</h1>"* ]]; then
				reason="error page: $title"
			fi
		done
	else
		compact="$(jq -c . <<<"$body" 2>/dev/null || true)"
		for known in "${ERROR_JSON_BODIES[@]}"; do
			if [[ "$compact" == "$known" ]]; then
				reason="error response: $known"
			fi
		done
		if [[ -z "$reason" ]] && ! jq -e 'type == "object" and has("error")' <<<"$body" >/dev/null 2>&1; then
			continue
		fi
	fi
	if [[ -z "$reason" ]]; then
		UNKNOWN_ERRORS+=("$name: $(head -c 300 <<<"$body" | tr '\n' ' ')")
		continue
	fi
	mv "$f" "$REMOVED_DIR/$name"
	printf '%s\t%s\n' "$name" "$reason" >>"$REMOVED_LIST"
	REMOVED_FROM_OLD=$((REMOVED_FROM_OLD + 1))
done < <(find "$SCRATCH/old/$TOP_DIR" -maxdepth 1 -type f -name '*.json' -print0)
if [[ "${#UNKNOWN_ERRORS[@]}" -gt 0 ]]; then
	echo "🔴 unrecognised error responses in old archive (review, then add to ERROR_PAGE_TITLES or ERROR_JSON_BODIES if they should be removed):" >&2
	printf '%s\n' "${UNKNOWN_ERRORS[@]}" >&2
	exit 1
fi
echo "Removed $REMOVED_FROM_OLD saved error responses (moved to $REMOVED_DIR, listed in removed.tsv)"
if [[ "$REMOVED_FROM_OLD" -gt 0 ]]; then
	cut -f2 "$REMOVED_LIST" | sort | uniq -c
fi

echo "🔵 Staging old archive files by date"
STAGE_YEAR_DIR="$SCRATCH/stage/$YEAR_PATH"
mkdir -p "$STAGE_YEAR_DIR"
STAGED_FROM_OLD=0
# Latest staged name. Names are <dataset>-<date>_<HH-MM-SS>.json, so they sort chronologically.
OLD_LAST_NAME=""
while IFS= read -r -d '' f; do
	name="$(basename "$f")"
	if [[ ! "$name" =~ _([0-9]{4}-[0-9]{2}-[0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]{2})\.json$ ]]; then
		echo "🔴 could not extract a date/time from filename: $name" >&2
		exit 1
	fi
	date="${BASH_REMATCH[1]}"
	time="${BASH_REMATCH[2]}-${BASH_REMATCH[3]}-${BASH_REMATCH[4]}"
	if [[ "${date:0:4}" != "$YEAR" ]]; then
		echo "🔴 file date $date does not belong to year $YEAR: $name" >&2
		exit 1
	fi
	new_name="${DATASET}-${date}_${time}.json"
	day_dir="$STAGE_YEAR_DIR/$date"
	mkdir -p "$day_dir"
	if [[ -e "$day_dir/$new_name" ]]; then
		echo "🔴 duplicate file would overwrite staged data: $day_dir/$new_name" >&2
		exit 1
	fi
	old_mtime="$(stat -c %Y "$f")"
	mv "$f" "$day_dir/$new_name"
	new_mtime="$(stat -c %Y "$day_dir/$new_name")"
	if [[ "$old_mtime" != "$new_mtime" ]]; then
		echo "🔴 mtime changed on rename: $name -> $new_name ($old_mtime != $new_mtime)" >&2
		exit 1
	fi
	if [[ "$new_name" > "$OLD_LAST_NAME" ]]; then
		OLD_LAST_NAME="$new_name"
	fi
	STAGED_FROM_OLD=$((STAGED_FROM_OLD + 1))
done < <(find "$SCRATCH/old/$TOP_DIR" -maxdepth 1 -type f -name '*.json' -print0)
if [[ $((STAGED_FROM_OLD + REMOVED_FROM_OLD)) -ne "$OLD_FILE_COUNT" ]]; then
	echo "🔴 staged $STAGED_FROM_OLD and removed $REMOVED_FROM_OLD files from old archive, expected $OLD_FILE_COUNT in total" >&2
	exit 1
fi
echo "Staged $STAGED_FROM_OLD files from the old archive"

echo "🔵 Checking for a live history/$YEAR/ leftover on the remote"
YEAR_INDEX_STATUS="$(curl -s -m 60 -o /dev/null -w '%{http_code}' "$YEAR_INDEX_URL" || true)"
STAGED_FROM_LIVE=0
DAYS=""
if [[ "$YEAR_INDEX_STATUS" == "404" ]]; then
	echo "No live history/$YEAR/ leftover (404) — nothing to merge"
elif [[ "$YEAR_INDEX_STATUS" != "200" ]]; then
	echo "🔴 unexpected HTTP $YEAR_INDEX_STATUS for $YEAR_INDEX_URL" >&2
	exit 1
else
	# Everything that ends up deletable from the remote is recorded from this listing, so it is
	# checked strictly: anything the rollup layout wouldn't have (see scripts/rollup/02-list.sh)
	# aborts rather than being skipped or guessed at.
	echo "🔵 Checking the live listing"
	fetch_index "$YEAR_INDEX_URL" "$LIVE_DIR/index.json"
	if ! jq -e --arg p "$YEAR_PATH" '.path == $p' "$LIVE_DIR/index.json" >/dev/null; then
		echo "🔴 $YEAR_INDEX_URL is not the index of $YEAR_PATH" >&2
		exit 1
	fi
	YEAR_ROOT_FILES="$(jq -r '(.files // [])[].name' "$LIVE_DIR/index.json")"
	if [[ -n "$YEAR_ROOT_FILES" ]]; then
		echo "🔴 files directly under history/$YEAR/, outside any day dir:" >&2
		echo "$YEAR_ROOT_FILES" >&2
		exit 1
	fi
	DAYS="$(jq -r '(.directories // [])[].name' "$LIVE_DIR/index.json" | LC_ALL=C sort)"
	if [[ -z "$DAYS" ]]; then
		echo "🔴 live history/$YEAR/ exists but index.json lists no day directories" >&2
		exit 1
	fi
	DUPLICATE_DAYS="$(uniq -d <<<"$DAYS")"
	if [[ -n "$DUPLICATE_DAYS" ]]; then
		echo "🔴 day directories listed more than once under history/$YEAR/:" >&2
		echo "$DUPLICATE_DAYS" >&2
		exit 1
	fi
	while IFS= read -r day; do
		if [[ ! "$day" =~ ^${YEAR}-[0-9]{2}-[0-9]{2}$ || "$(date -u -d "$day" +%F 2>/dev/null || true)" != "$day" ]]; then
			echo "🔴 unexpected directory under history/$YEAR/: $day" >&2
			exit 1
		fi
	done <<<"$DAYS"
	fetch_day_indexes "$LIVE_DIR"
	echo "Live leftover found: $(wc -l <<<"$DAYS") days, $(head -n1 <<<"$DAYS") to $(tail -n1 <<<"$DAYS")"

	echo "🔵 Downloading and staging the live leftover"
	# live-files.tsv, one row per live file: remote path, size, MD5, SHA-1 (of the .bz2 as stored
	# on the remote), last_modified, path in the new archive, sha256 (of the decompressed file).
	: >"$LIVE_MANIFEST"
	while IFS= read -r day; do
		if ! jq -e --arg p "$YEAR_PATH/$day" '.path == $p and ((.directories // []) | length) == 0' \
			"$LIVE_DIR/$day/index.json" >/dev/null; then
			echo "🔴 unexpected index for history/$YEAR/$day (wrong path, or has subdirectories)" >&2
			exit 1
		fi
		FILE_ROWS="$(jq -r '(.files // [])[] | [.name, .url, .size, .etag, .last_modified] | map(tostring) | @tsv' \
			"$LIVE_DIR/$day/index.json")"
		if [[ -z "$FILE_ROWS" ]]; then
			echo "🔴 no data files listed for history/$YEAR/$day" >&2
			exit 1
		fi
		day_dir="$STAGE_YEAR_DIR/$day"
		mkdir -p "$day_dir"
		while IFS=$'\t' read -r name url size etag last_modified; do
			if [[ ! "$name" =~ ^${DATASET}-${day}_[0-9]{2}-[0-9]{2}-[0-9]{2}(\.v[0-9]+)?\.json\.bz2$ ]]; then
				echo "🔴 unexpected file in history/$YEAR/$day: $name" >&2
				exit 1
			fi
			if [[ "$url" != "$PUBLIC_BASE_URL/$YEAR_PATH/$day/$name" ]]; then
				echo "🔴 unexpected url for $name: $url" >&2
				exit 1
			fi
			if [[ ! "$size" =~ ^[1-9][0-9]*$ || ! "$etag" =~ ^[0-9a-f]{32}$ ]]; then
				echo "🔴 missing or bad size/etag for $name: size=$size etag=$etag" >&2
				exit 1
			fi
			if [[ ! "$last_modified" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(\.[0-9]+)?Z$ ]]; then
				echo "🔴 missing or bad last_modified for $name: $last_modified — cannot preserve its file time" >&2
				exit 1
			fi
			json_name="${name%.bz2}"
			if [[ -n "$OLD_LAST_NAME" && ! "$json_name" > "$OLD_LAST_NAME" ]]; then
				echo "🔴 live file $json_name does not come after the last old-archive file $OLD_LAST_NAME (expected the old archive to end before the live leftover starts)" >&2
				exit 1
			fi
			if [[ -e "$day_dir/$json_name" || -e "$day_dir/$name" ]]; then
				echo "🔴 live file would overwrite staged data: $day_dir/$json_name" >&2
				exit 1
			fi
			bz2_path="$day_dir/$name"
			if ! curl -fsS -m 120 --retry 5 -o "$bz2_path" "$url"; then
				echo "🔴 failed to download $url" >&2
				exit 1
			fi
			verify_download "$bz2_path" "$size" "$etag"
			sha1="$(sha1sum "$bz2_path" | cut -d' ' -f1)"
			if ! bzip2 -t "$bz2_path"; then
				echo "🔴 bzip2 integrity check failed: $bz2_path" >&2
				exit 1
			fi
			bunzip2 "$bz2_path"
			# last_modified is remote metadata (same field rollup's own mtime check trusts),
			# not derived from the JSON content, and its format was checked above.
			touch -d "$last_modified" "$day_dir/$json_name"
			sha256="$(sha256sum "$day_dir/$json_name" | cut -d' ' -f1)"
			printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$YEAR_PATH/$day/$name" "$size" "$etag" "$sha1" \
				"$last_modified" "$YEAR_PATH/$day/$json_name" "$sha256" >>"$LIVE_MANIFEST"
			STAGED_FROM_LIVE=$((STAGED_FROM_LIVE + 1))
		done <<<"$FILE_ROWS"
	done <<<"$DAYS"
	LC_ALL=C sort -o "$LIVE_MANIFEST" "$LIVE_MANIFEST"
	cut -f1 "$LIVE_MANIFEST" >"$SCRATCH/live-files.txt"
	LIVE_COUNT="$(wc -l <"$LIVE_MANIFEST")"
	if [[ "$LIVE_COUNT" -ne "$STAGED_FROM_LIVE" ]]; then
		echo "🔴 live-files.tsv has $LIVE_COUNT rows, staged $STAGED_FROM_LIVE live files" >&2
		exit 1
	fi
	echo "Staged $STAGED_FROM_LIVE files from the live history/$YEAR/ leftover, recorded in live-files.tsv"
	echo "Old archive ends at $OLD_LAST_NAME, live leftover starts at $(head -n1 "$LIVE_MANIFEST" | cut -f6 | xargs basename)"
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
	echo "🔵 Checking each file individually to find which ones are bad" >&2
	find "$STAGE_YEAR_DIR" -type f -print0 | xargs -0 -P"$(nproc)" -n1 bash -c '
		f="$1"
		out="$(jq -c . "$f" 2>&1)"
		rc=$?
		if [[ "$rc" -ne 0 ]]; then
			printf "🔴 %s: invalid JSON: %s\n" "$f" "$out" >&2
		elif [[ "$(wc -l <<<"$out")" -ne 1 ]]; then
			printf "🔴 %s: holds %s JSON values, expected exactly one\n" "$f" "$(wc -l <<<"$out")" >&2
		fi
	' bash
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

if [[ -n "$DAYS" ]]; then
	echo "🔵 Checking every live file is in the archive"
	MISSING_LIVE="$(comm -23 \
		<(awk -F'\t' '{print $7"  "$6}' "$LIVE_MANIFEST" | LC_ALL=C sort) \
		<(LC_ALL=C sort "$SCRATCH/archive-contents.sha256"))"
	if [[ -n "$MISSING_LIVE" ]]; then
		echo "🔴 live files missing from the archive, or with different content (sha256, path):" >&2
		echo "$MISSING_LIVE" >&2
		exit 1
	fi
	echo "All $STAGED_FROM_LIVE live files are in the archive with the sha256 recorded in live-files.tsv"

	echo "🔵 Re-checking the live listing is unchanged since the start of the run"
	fetch_index "$YEAR_INDEX_URL" "$SCRATCH/live-recheck/index.json"
	fetch_day_indexes "$SCRATCH/live-recheck"
	LISTING_DIFF="$(diff <(normalize_listing "$LIVE_DIR") <(normalize_listing "$SCRATCH/live-recheck") || true)"
	if [[ -n "$LISTING_DIFF" ]]; then
		echo "🔴 live listing changed during the run (first fetch <, re-fetch >):" >&2
		echo "$LISTING_DIFF" >&2
		exit 1
	fi
	echo "Live listing unchanged"
fi

echo "🔵 Writing archive checksums"
sha256sum "$NEW_ARCHIVE_PATH" | awk -v n="$NEW_ARCHIVE_NAME" '{print $1"  "n}' >"$SCRATCH/archive.sha256"
sha1sum "$NEW_ARCHIVE_PATH" | awk -v n="$NEW_ARCHIVE_NAME" '{print $1"  "n}' >"$SCRATCH/archive.sha1"
echo "Wrote archive.sha256, archive.sha1"

echo "✅ rebuild-archive OK for $DATASET $YEAR"
echo "Result: $NEW_ARCHIVE_PATH"
echo "Removed error responses: $REMOVED_FROM_OLD (see removed.tsv)"
if [[ -n "$DAYS" ]]; then
	echo "Live leftover: $STAGED_FROM_LIVE files archived, listed in live-files.tsv and live-files.txt."
	echo "When clearing history/$YEAR/, delete only the files in live-files.txt, never the whole dir"
	echo "(see README.md, \"Clearing a live leftover\")."
fi
echo "This script never uploads or touches the remote — review the archive and checksums in"
echo "$SCRATCH, then upload $NEW_ARCHIVE_NAME yourself."
