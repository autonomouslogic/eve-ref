#!/usr/bin/env bash
# Step 2: list the remote year dir, check for anomalies, report what's there.
# Usage: ./10-list.sh <archive> <year>
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh" "$@"

echo "## 🔵 Listing"
rclone lsjson -R --hash --files-only --fast-list "$REMOTE/$YEAR_PATH" >"$SCRATCH/listing.json"

TOTAL_COUNT="$(jq 'length' "$SCRATCH/listing.json")"
if [[ "$TOTAL_COUNT" -eq 0 ]]; then
	echo "🔴 No files found under $YEAR_PATH" >&2
	exit 1
fi
echo "Found $TOTAL_COUNT files"

# Data file: <year>-MM-DD/<archive>-<year>-MM-DD_HH-mm-ss[.vN].json.bz2, with the directory's
# year anchored to $YEAR. Index file: index.html or index.json, at the year root or one per day.
DATA_RE="^(?<dirdate>${YEAR}-[0-9]{2}-[0-9]{2})/${ARCHIVE}-(?<filedate>[0-9]{4}-[0-9]{2}-[0-9]{2})_(?<time>[0-9]{2}-[0-9]{2}-[0-9]{2})(\\.v[0-9]+)?\\.json\\.bz2\$"
INDEX_RE="^((?<idxdate>${YEAR}-[0-9]{2}-[0-9]{2})/)?index\\.(html|json)\$"

echo "## 🔵 Checking for anomalies"
REPORT_JSON="$(jq \
	--arg data_re "$DATA_RE" \
	--arg index_re "$INDEX_RE" \
	'
	def is_data: test($data_re);
	def is_index: test($index_re);

	. as $all
	| ([$all[] | select(.Path | is_data)]) as $data
	| ([$data[] | (.Path | capture($data_re))]) as $data_cap
	| ([$data_cap[] | .dirdate + "T" + (.time | gsub("-"; ":"))]) as $timestamps
	| {
		total_count: ($all | length),
		total_bytes: ([$all[].Size] | add // 0),
		zero_byte_paths: [$all[] | select(.Size == 0) | .Path],
		missing_hash_paths: [$all[] | select((.Hashes.sha1 // "") == "") | .Path],
		bad_paths: [$all[] | select(((.Path | is_data) or (.Path | is_index)) | not) | .Path],
		mismatched_date_paths: [$data_cap[] | select(.dirdate != .filedate) | (.dirdate + "/" + .filedate)],
		data_count: ($data | length),
		data_bytes: ([$data[].Size] | add // 0),
		sample_path: ($data[0].Path // null),
		index_html_count: ([$all[] | select(.Path | test("index\\.html$"))] | length),
		index_json_count: ([$all[] | select(.Path | test("index\\.json$"))] | length),
		day_counts: ([$data_cap[].dirdate] | group_by(.) | map({(.[0]): length}) | add // {}),
		first_timestamp: ($timestamps | min // null),
		last_timestamp: ($timestamps | max // null)
	}
	' "$SCRATCH/listing.json")"

ANOMALY_COUNT=0
for field in zero_byte_paths missing_hash_paths bad_paths mismatched_date_paths; do
	count="$(jq --arg f "$field" '.[$f] | length' <<<"$REPORT_JSON")"
	if [[ "$count" -gt 0 ]]; then
		echo "🔴 Anomaly/anomalies ($count): $field" >&2
		jq -r --arg f "$field" '.[$f][]' <<<"$REPORT_JSON" >&2
		ANOMALY_COUNT=$((ANOMALY_COUNT + count))
	fi
done

DAY_COUNT="$(jq '.day_counts | length' <<<"$REPORT_JSON")"
INDEX_HTML_COUNT="$(jq '.index_html_count' <<<"$REPORT_JSON")"
INDEX_JSON_COUNT="$(jq '.index_json_count' <<<"$REPORT_JSON")"
EXPECTED_INDEX_COUNT=$((DAY_COUNT + 1))

if [[ "$INDEX_HTML_COUNT" -ne "$EXPECTED_INDEX_COUNT" ]]; then
	echo "🔴 Expected $EXPECTED_INDEX_COUNT index.html files (one per day plus the year root), found $INDEX_HTML_COUNT" >&2
	ANOMALY_COUNT=$((ANOMALY_COUNT + 1))
fi
if [[ "$INDEX_JSON_COUNT" -ne "$EXPECTED_INDEX_COUNT" ]]; then
	echo "🔴 Expected $EXPECTED_INDEX_COUNT index.json files (one per day plus the year root), found $INDEX_JSON_COUNT" >&2
	ANOMALY_COUNT=$((ANOMALY_COUNT + 1))
fi

if [[ "$ANOMALY_COUNT" -gt 0 ]]; then
	echo "🔴 Listing has anomalies, aborting" >&2
	exit 1
fi

echo "## 🔵 Summary"
PRESENT_DAYS="$(jq -r '.day_counts | keys[]' <<<"$REPORT_JSON" | sort)"
ALL_DAYS="$(
	d="$YEAR-01-01"
	end="$((YEAR + 1))-01-01"
	while [[ "$d" < "$end" ]]; do
		echo "$d"
		d="$(date -u -d "$d + 1 day" +%F)"
	done
)"
MISSING_DAYS="$(comm -23 <(echo "$ALL_DAYS") <(echo "$PRESENT_DAYS") || true)"
MISSING_COUNT=0
if [[ -n "$MISSING_DAYS" ]]; then
	MISSING_COUNT="$(wc -l <<<"$MISSING_DAYS")"
fi

MIN_PER_DAY="$(jq '.day_counts | to_entries | map(.value) | min' <<<"$REPORT_JSON")"
MAX_PER_DAY="$(jq '.day_counts | to_entries | map(.value) | max' <<<"$REPORT_JSON")"
TOTAL_BYTES="$(jq '.total_bytes' <<<"$REPORT_JSON")"
DATA_COUNT="$(jq '.data_count' <<<"$REPORT_JSON")"
FIRST_TIMESTAMP="$(jq -r '.first_timestamp' <<<"$REPORT_JSON")"
LAST_TIMESTAMP="$(jq -r '.last_timestamp' <<<"$REPORT_JSON")"

echo "Total files: $TOTAL_COUNT ($DATA_COUNT data, $INDEX_HTML_COUNT index.html, $INDEX_JSON_COUNT index.json)"
echo "Total bytes: $TOTAL_BYTES"
echo "Days covered: $DAY_COUNT (files per day: min $MIN_PER_DAY, max $MAX_PER_DAY)"
echo "Missing days: $MISSING_COUNT"
if [[ "$MISSING_COUNT" -gt 0 ]]; then
	echo "$MISSING_DAYS"
fi
echo "First timestamp: $FIRST_TIMESTAMP"
echo "Last timestamp: $LAST_TIMESTAMP"

echo "## 🔵 Free space"
# Reuses this listing instead of a separate "rclone size" / recursive "rclone lsf": the byte
# counts are already in $REPORT_JSON. Only the one sample download below is extra remote traffic,
# needed to estimate the bz2 decompression ratio.
DATA_BYTES="$(jq '.data_bytes' <<<"$REPORT_JSON")"
SAMPLE_PATH="$(jq -r '.sample_path' <<<"$REPORT_JSON")"
if [[ "$SAMPLE_PATH" == "null" ]]; then
	echo "🔴 No data files found under $YEAR_PATH to sample" >&2
	exit 1
fi

SAMPLE_TMP="$(mktemp)"
trap 'rm -f "$SAMPLE_TMP"' EXIT
rclone cat "$REMOTE/$YEAR_PATH/$SAMPLE_PATH" >"$SAMPLE_TMP"
COMPRESSED_SIZE="$(stat -c%s "$SAMPLE_TMP")"
if [[ "$COMPRESSED_SIZE" -eq 0 ]]; then
	echo "🔴 Sampled file is empty: $SAMPLE_PATH" >&2
	exit 1
fi
DECOMPRESSED_SIZE="$(bunzip2 -c "$SAMPLE_TMP" | wc -c)"
rm -f "$SAMPLE_TMP"
trap - EXIT

RATIO="$(awk -v d="$DECOMPRESSED_SIZE" -v c="$COMPRESSED_SIZE" 'BEGIN { print d / c }')"
ESTIMATED_DECOMPRESSED_BYTES="$(awk -v b="$DATA_BYTES" -v r="$RATIO" 'BEGIN { printf "%d", b * r }')"
REQUIRED_BYTES=$((2 * TOTAL_BYTES + 2 * ESTIMATED_DECOMPRESSED_BYTES))

mkdir -p "$ROLLUP_SCRATCH_ROOT"
AVAILABLE_BYTES="$(df -B1 --output=avail "$ROLLUP_SCRATCH_ROOT" | tail -n1 | tr -d ' ')"

echo "Sampled file: $SAMPLE_PATH (compressed $COMPRESSED_SIZE, decompressed $DECOMPRESSED_SIZE, ratio $RATIO)"
echo "Estimated decompressed year size: $ESTIMATED_DECOMPRESSED_BYTES bytes"
echo "Required free space (2x remote + 2x estimated decompressed): $REQUIRED_BYTES bytes"
echo "Available free space under $ROLLUP_SCRATCH_ROOT: $AVAILABLE_BYTES bytes"

if [[ "$AVAILABLE_BYTES" -lt "$REQUIRED_BYTES" ]]; then
	echo "🔴 Not enough free space under $ROLLUP_SCRATCH_ROOT" >&2
	exit 1
fi

mark_done \
	"total_count=$TOTAL_COUNT" \
	"data_count=$DATA_COUNT" \
	"total_bytes=$TOTAL_BYTES" \
	"days_covered=$DAY_COUNT" \
	"missing_days=$MISSING_COUNT" \
	"first_timestamp=$FIRST_TIMESTAMP" \
	"last_timestamp=$LAST_TIMESTAMP" \
	"estimated_decompressed_bytes=$ESTIMATED_DECOMPRESSED_BYTES" \
	"required_bytes=$REQUIRED_BYTES" \
	"available_bytes=$AVAILABLE_BYTES"

echo "✅ List OK for $ARCHIVE $YEAR"
