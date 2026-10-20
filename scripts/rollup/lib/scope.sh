# Rollup scope. Sourced by common.sh and advise.sh. See NOTES.md, "Scope".
ROLLUP_SCOPE=(
  faction-warfare-leaderboards
  faction-warfare-leaderboards-characters
  faction-warfare-leaderboards-corporations
  faction-warfare-stats
  faction-warfare-systems
  faction-warfare-wars
  freelance-jobs
  industry-facilities
  industry-systems
  insurance-prices
  markets-prices
  military-campaigns
  shadow-war-arc-goals
  skinr-details
  skinr-listings
  sovereignty-campaigns
  sovereignty-map
  sovereignty-structures
  structures
  system-jumps
  system-kills
  universe-structures
  warzone
  warzone-insurgency
  warzone-leaderboard
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
