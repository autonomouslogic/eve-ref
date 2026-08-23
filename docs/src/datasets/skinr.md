# SKINR

SKINR is EVE Online's cosmetic customization system, available via the Paragon Hub.

* [data.everef.net/skinr-listings/](https://data.everef.net/skinr-listings/) — active SKINR listings
* [data.everef.net/skinr-details/](https://data.everef.net/skinr-details/) — SKINR cosmetic details

Produced by the [scrape-skinr](../commands/scrape-skinr.md) command.

## SKINR listings

Contains all currently active SKINR listings from the Paragon Hub marketplace.
The `latest.json` file is a JSON object with the following structure:

```json
{
  "listings": [ ... ],
  "cursor": { "after": "<after-cursor>" }
}
```

Each listing entry is the raw object returned by the ESI [`/paragon-hub/skinr`](https://developers.eveonline.com/api-explorer#/operations/GetParagonHubSkinr) endpoint.
The `cursor` field tracks the incremental fetch position and is used by subsequent scrapes to fetch only new listings.

Archived files are bzip2-compressed (`.json.bz2`). The latest file is uncompressed JSON.

## SKINR details

Contains detail objects for each SKINR skin referenced by the active listings.
The files are JSON objects keyed by `skinr_id`:

```json
{
  "9309c0...": { ... },
  "c44eb1...": { ... }
}
```

Each value is the raw object returned by the ESI [`/cosmetics/skinr/{skinr_id}`](https://developers.eveonline.com/api-explorer#/operations/GetCosmeticsSkinr) endpoint.
Details are only kept for listings in `skinr-listings`.
Orphaned entries are pruned om each run.

Archived files are bzip2-compressed (`.json.bz2`). The latest file is uncompressed JSON.
