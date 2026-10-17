# Rollup scope. Sourced by common.sh and advise.sh. See NOTES.md, "Scope".
ROLLUP_SCOPE=(
	incursions
	skinr-listings
	skinr-details
	sovereignty-*
	industry-*
	system-jumps
	system-kills
	warzone*
	insurance-prices
	universe-structures
	faction-warfare-*
	military-campaigns
	markets-prices
	freelance-jobs
	structures
)

in_rollup_scope() {
	local archive="$1" pattern
	for pattern in "${ROLLUP_SCOPE[@]}"; do
		if [[ "$archive" == $pattern ]]; then
			return 0
		fi
	done
	return 1
}
