# Scrape SKINR

Produces the [SKINR datasets](../datasets/skinr.md).

Fetches active SKINR listings from the Paragon Hub via the ESI `/paragon-hub/skinr` endpoint,
then fetches cosmetic details for each listing via `/cosmetics/skinr/{skinr_id}`.

The scrape uses cursor-pagination and updated records are captured.
Olver listings no longer in `listed` state are purged, along with their orphaned detail entries.

Both output files are uploaded to S3 as the latest file and as a timestamped archive.
