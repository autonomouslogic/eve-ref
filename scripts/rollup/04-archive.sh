#!/usr/bin/env bash
# Step 5: build the tar.xz archive from stage/, verify headers, order, mtimes and contents.
# Usage: ./04-archive.sh <archive> <year>
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh" "$@"

STAGE_ROOT="$SCRATCH/stage"
ARCHIVE_PATH="$SCRATCH/$ARCHIVE_NAME"
ARCHIVE_TMP="$ARCHIVE_PATH.tmp"

if [[ -e "$ARCHIVE_PATH" ]]; then
	echo "🔴 archive already exists, remove it before retrying: $ARCHIVE_PATH" >&2
	exit 1
fi
rm -f "$ARCHIVE_TMP"

echo "## 🔵 Building archive"
(
	cd "$STAGE_ROOT"
	find "$YEAR_PATH" -type f -print0 \
		| LC_ALL=C sort -z \
		| tar --null --no-recursion -T - \
			--owner=0 --group=0 --numeric-owner --mode='a=r,u+w' \
			-cJf "$ARCHIVE_TMP"
)
mv "$ARCHIVE_TMP" "$ARCHIVE_PATH"
touch -d "${YEAR}-12-31T23:59:59Z" "$ARCHIVE_PATH"
echo "Built $ARCHIVE_PATH"

echo "## 🔵 Checking xz integrity"
xz -t "$ARCHIVE_PATH"
echo "xz -t passed"

echo "## 🔵 Checking headers and order"
TAR_LISTING="$(tar -tvf "$ARCHIVE_PATH" --full-time)"

BAD_MODE="$(awk '$1 != "-rw-r--r--"' <<<"$TAR_LISTING")"
if [[ -n "$BAD_MODE" ]]; then
	echo "🔴 Entries with unexpected mode (want -rw-r--r--):" >&2
	echo "$BAD_MODE" >&2
	exit 1
fi

BAD_OWNER="$(awk '$2 != "0/0"' <<<"$TAR_LISTING")"
if [[ -n "$BAD_OWNER" ]]; then
	echo "🔴 Entries with unexpected owner (want 0/0, empty uname/gname):" >&2
	echo "$BAD_OWNER" >&2
	exit 1
fi

TAR_NAMES="$(awk '{print $6}' <<<"$TAR_LISTING")"
if ! LC_ALL=C sort -c <<<"$TAR_NAMES"; then
	echo "🔴 Archive entries are not in LC_ALL=C sorted order" >&2
	exit 1
fi

TAR_COUNT="$(wc -l <<<"$TAR_LISTING")"
STAGE_FILE_COUNT="$(cd "$STAGE_ROOT" && find "$YEAR_PATH" -type f | wc -l)"
if [[ "$TAR_COUNT" -ne "$STAGE_FILE_COUNT" ]]; then
	echo "🔴 Archive has $TAR_COUNT entries, stage has $STAGE_FILE_COUNT files" >&2
	exit 1
fi
echo "All $TAR_COUNT entries are regular files, mode -rw-r--r--, owner 0/0, sorted order"

echo "## 🔵 Checking names, sizes and mtimes against stage"
TAR_TSV="$(awk '{print $6"\t"$3"\t"$4" "$5}' <<<"$TAR_LISTING")"
STAGE_TSV="$(
	cd "$STAGE_ROOT"
	while IFS= read -r -d '' f; do
		size="$(stat -c%s "$f")"
		mtime="$(date -u -d "@$(stat -c%Y "$f")" +'%Y-%m-%d %H:%M:%S')"
		printf '%s\t%s\t%s\n' "$f" "$size" "$mtime"
	done < <(find "$YEAR_PATH" -type f -print0)
)"
TSV_DIFF="$(diff <(LC_ALL=C sort <<<"$TAR_TSV") <(LC_ALL=C sort <<<"$STAGE_TSV") || true)"
if [[ -n "$TSV_DIFF" ]]; then
	echo "🔴 Archive entries differ from stage (name, size, mtime):" >&2
	echo "$TSV_DIFF" >&2
	exit 1
fi
echo "Names, sizes and mtimes match stage exactly"

echo "## 🔵 Checking contents hash-match the originals"
tar -xJf "$ARCHIVE_PATH" \
	--to-command='printf "%s  %s\n" "$(sha256sum | cut -d" " -f1)" "$TAR_FILENAME"' \
	>"$SCRATCH/archive-contents.sha256"
CONTENTS_DIFF="$(diff "$SCRATCH/$ARCHIVE-$YEAR.sha256" "$SCRATCH/archive-contents.sha256" || true)"
if [[ -n "$CONTENTS_DIFF" ]]; then
	echo "🔴 archive-contents.sha256 differs from $ARCHIVE-$YEAR.sha256:" >&2
	echo "$CONTENTS_DIFF" >&2
	exit 1
fi
echo "archive-contents.sha256 matches $ARCHIVE-$YEAR.sha256"

echo "## 🔵 Writing archive checksums"
sha256sum "$ARCHIVE_PATH" | awk -v n="$ARCHIVE_NAME" '{print $1"  "n}' >"$SCRATCH/archive.sha256"
sha1sum "$ARCHIVE_PATH" | awk -v n="$ARCHIVE_NAME" '{print $1"  "n}' >"$SCRATCH/archive.sha1"
ARCHIVE_BYTES="$(stat -c%s "$ARCHIVE_PATH")"
ARCHIVE_SHA256="$(cut -d' ' -f1 "$SCRATCH/archive.sha256")"
ARCHIVE_SHA1="$(cut -d' ' -f1 "$SCRATCH/archive.sha1")"

mark_done \
	"entries=$TAR_COUNT" \
	"archive_bytes=$ARCHIVE_BYTES" \
	"archive_sha256=$ARCHIVE_SHA256" \
	"archive_sha1=$ARCHIVE_SHA1"

echo "✅ Archive OK for $ARCHIVE $YEAR"
