#!/usr/bin/env bash
# Step 6: upload the archive, verify via B2 (rclone) and the public URL.
# Usage: ./05-upload.sh <archive> <year>
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh" "$@"

ARCHIVE_PATH="$SCRATCH/$ARCHIVE_NAME"
REMOTE_ARCHIVE_PATH="$REMOTE/$ARCHIVE/history/$ARCHIVE_NAME"
PUBLIC_ARCHIVE_URL="$PUBLIC_BASE_URL/$ARCHIVE/history/$ARCHIVE_NAME"

LOCAL_SHA1="$(cut -d' ' -f1 "$SCRATCH/archive.sha1")"
LOCAL_SHA256="$(cut -d' ' -f1 "$SCRATCH/archive.sha256")"
LOCAL_BYTES="$(stat -c%s "$ARCHIVE_PATH")"
EXPECTED_MODTIME="${YEAR}-12-31T23:59:59Z"

echo "## 🔵 Checking for an existing remote object"
EXISTING_JSON="$(rclone lsjson --hash "$REMOTE/$ARCHIVE/history/" | jq --arg n "$ARCHIVE_NAME" '[.[] | select(.Name == $n)] | .[0] // null')"
if [[ "$EXISTING_JSON" != "null" ]]; then
	EXISTING_SHA1="$(jq -r '.Hashes.sha1 // ""' <<<"$EXISTING_JSON")"
	if [[ "$EXISTING_SHA1" != "$LOCAL_SHA1" ]]; then
		echo "🔴 Remote already has $ARCHIVE_NAME with a different SHA-1 (remote $EXISTING_SHA1, local $LOCAL_SHA1)" >&2
		exit 1
	fi
	echo "Remote already has $ARCHIVE_NAME with matching SHA-1, skipping upload"
else
	echo "## 🔵 Uploading"
	rclone copyto --immutable "$ARCHIVE_PATH" "$REMOTE_ARCHIVE_PATH"
	echo "Uploaded $ARCHIVE_PATH to $REMOTE_ARCHIVE_PATH"
fi

echo "## 🔵 Checking the remote object"
REMOTE_JSON="$(rclone lsjson --hash "$REMOTE/$ARCHIVE/history/" | jq --arg n "$ARCHIVE_NAME" '[.[] | select(.Name == $n)] | .[0] // null')"
if [[ "$REMOTE_JSON" == "null" ]]; then
	echo "🔴 $ARCHIVE_NAME not found on the remote after upload" >&2
	exit 1
fi

REMOTE_SIZE="$(jq -r '.Size' <<<"$REMOTE_JSON")"
REMOTE_SHA1="$(jq -r '.Hashes.sha1 // ""' <<<"$REMOTE_JSON")"
REMOTE_MODTIME="$(jq -r '.ModTime | sub("\\.[0-9]+Z$"; "Z")' <<<"$REMOTE_JSON")"

if [[ "$REMOTE_SIZE" -ne "$LOCAL_BYTES" ]]; then
	echo "🔴 Remote size $REMOTE_SIZE does not match local size $LOCAL_BYTES" >&2
	exit 1
fi
if [[ "$REMOTE_SHA1" != "$LOCAL_SHA1" ]]; then
	echo "🔴 Remote SHA-1 $REMOTE_SHA1 does not match local SHA-1 $LOCAL_SHA1" >&2
	exit 1
fi
if [[ "$REMOTE_MODTIME" != "$EXPECTED_MODTIME" ]]; then
	echo "🔴 Remote ModTime $REMOTE_MODTIME does not equal $EXPECTED_MODTIME" >&2
	exit 1
fi
echo "Remote object OK: size $REMOTE_SIZE, SHA-1 $REMOTE_SHA1, ModTime $REMOTE_MODTIME"

echo "## 🔵 Checking the public URL"
# The public URL can take a few seconds to become servable right after an upload, so retry.
STATUS_LINE=""
for attempt in 1 2 3 4 5 6; do
	HEADERS="$(curl -sI --max-time 30 "$PUBLIC_ARCHIVE_URL" || true)"
	STATUS_LINE="$(head -n1 <<<"$HEADERS" | tr -d '\r')"
	if [[ "$STATUS_LINE" == *" 200"* ]]; then
		break
	fi
	echo "Public URL not ready yet (attempt $attempt): ${STATUS_LINE:-<no response>}"
	sleep 5
done
if [[ "$STATUS_LINE" != *" 200"* ]]; then
	echo "🔴 Expected HTTP 200 for $PUBLIC_ARCHIVE_URL, got: ${STATUS_LINE:-<no response>}" >&2
	exit 1
fi

header_value() {
	grep -i "^$1:" <<<"$HEADERS" | tail -n1 | cut -d: -f2- | tr -d '\r' | sed 's/^ *//' || true
}

CONTENT_LENGTH="$(header_value 'content-length')"
LAST_MODIFIED="$(header_value 'last-modified')"
SRC_LAST_MODIFIED_MILLIS="$(header_value 'x-amz-meta-src_last_modified_millis')"

EXPECTED_LAST_MODIFIED="$(date -u -d "$EXPECTED_MODTIME" +'%a, %d %b %Y %H:%M:%S GMT')"
EXPECTED_MILLIS="$(($(date -u -d "$EXPECTED_MODTIME" +%s) * 1000))"

if [[ "$CONTENT_LENGTH" != "$LOCAL_BYTES" ]]; then
	echo "🔴 content-length $CONTENT_LENGTH does not match local size $LOCAL_BYTES" >&2
	exit 1
fi
if [[ "$LAST_MODIFIED" != "$EXPECTED_LAST_MODIFIED" ]]; then
	echo "🔴 last-modified '$LAST_MODIFIED' does not equal '$EXPECTED_LAST_MODIFIED'" >&2
	exit 1
fi
if [[ "$SRC_LAST_MODIFIED_MILLIS" != "$EXPECTED_MILLIS" ]]; then
	echo "🔴 x-amz-meta-src_last_modified_millis '$SRC_LAST_MODIFIED_MILLIS' does not equal '$EXPECTED_MILLIS'" >&2
	exit 1
fi
echo "Public headers OK: 200, content-length $CONTENT_LENGTH, last-modified '$LAST_MODIFIED'"

echo "## 🔵 Downloading the public copy"
mkdir -p "$SCRATCH/verify/public"
VERIFY_PATH="$SCRATCH/verify/public/$ARCHIVE_NAME"
if ! wget -nv -O "$VERIFY_PATH" "$PUBLIC_ARCHIVE_URL"; then
	echo "🔴 wget failed to download $PUBLIC_ARCHIVE_URL" >&2
	exit 1
fi

PUBLIC_SHA256="$(sha256sum "$VERIFY_PATH" | cut -d' ' -f1)"
if [[ "$PUBLIC_SHA256" != "$LOCAL_SHA256" ]]; then
	echo "🔴 Public download SHA-256 $PUBLIC_SHA256 does not match $LOCAL_SHA256" >&2
	exit 1
fi
echo "Public download SHA-256 matches archive.sha256"

PUBLIC_MTIME="$(date -u -d "@$(stat -c%Y "$VERIFY_PATH")" +%Y-%m-%dT%H:%M:%SZ)"
if [[ "$PUBLIC_MTIME" != "$EXPECTED_MODTIME" ]]; then
	echo "🔴 Downloaded file mtime $PUBLIC_MTIME does not equal $EXPECTED_MODTIME" >&2
	exit 1
fi
echo "Downloaded file mtime matches $EXPECTED_MODTIME"

mark_done \
	"remote_path=$ARCHIVE/history/$ARCHIVE_NAME" \
	"remote_sha1=$REMOTE_SHA1" \
	"public_sha256=$PUBLIC_SHA256"

echo "✅ Upload OK for $ARCHIVE $YEAR"
