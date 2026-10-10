#!/usr/bin/env bash
# Step 1: config, tools, remote access, year dir present, archive absent.
# Free space is checked in 01-list.sh, off the listing it already has to fetch there.
# Usage: ./00-preflight.sh <archive> <year>
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh" "$@"

echo "## 🔵 Tools"
for tool in rclone tar xz bzip2 jq sha256sum sha1sum curl find sort flock; do
	if ! command -v "$tool" >/dev/null 2>&1; then
		echo "🔴 Missing required tool: $tool" >&2
		exit 1
	fi
done
if ! tar --version | head -n1 | grep -q 'GNU tar'; then
	echo "🔴 Tar must be GNU tar" >&2
	exit 1
fi
rclone version | head -n1
tar --version | head -n1
echo "All required tools present"

echo "## 🔵 Remote layout"
REMOTE_HISTORY_LISTING="$(rclone lsf "$REMOTE/$ARCHIVE/history/")"
if ! grep -qx "$YEAR/" <<<"$REMOTE_HISTORY_LISTING"; then
	echo "🔴 Remote year dir not found: $REMOTE/$YEAR_PATH" >&2
	exit 1
fi
NEXT_YEAR=$((YEAR + 1))
if ! grep -qx "$NEXT_YEAR/" <<<"$REMOTE_HISTORY_LISTING"; then
	echo "🔴 Remote dir for the next year ($NEXT_YEAR) not found: $REMOTE/$ARCHIVE/history/$NEXT_YEAR" >&2
	echo "🔴 $YEAR is not confirmed finished: scraping hasn't moved on to $NEXT_YEAR yet" >&2
	exit 1
fi
if grep -qx "$ARCHIVE_NAME" <<<"$REMOTE_HISTORY_LISTING"; then
	echo "🔴 Archive already exists on the remote: $REMOTE/$ARCHIVE/history/$ARCHIVE_NAME" >&2
	exit 1
fi

PUBLIC_ARCHIVE_URL="$PUBLIC_BASE_URL/$ARCHIVE/history/$ARCHIVE_NAME"
HTTP_STATUS="$(curl -s -o /dev/null -w '%{http_code}' "$PUBLIC_ARCHIVE_URL")"
if [[ "$HTTP_STATUS" != "404" ]]; then
	echo "🔴 Expected 404 for $PUBLIC_ARCHIVE_URL, got $HTTP_STATUS" >&2
	exit 1
fi
echo "Remote year dir present, next year's dir present (year is finished), archive absent (remote and public)"

mark_done

echo "✅ Preflight OK for $ARCHIVE $YEAR"
