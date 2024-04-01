# Structures

[data.everef.net/structures/](https://data.everef.net/structures/) contains scrapes of the structures ESI endpoints.

These are:
* `/universe/structures/`
* `/universe/structures/{structure_id}/`
* `/sovereignty/structures/`

Queries are also made to `/markets/structures/{structure_id}/` to determine if there's a market present at the structure.

The v2 scrape files are single JSON files containing an object with the structure IDs as keys.
The object values themselves are verbatim from the structure endpoint, with the following additional fields:
* `structure_id`
* `constellation_id`
* `region_id`
* `is_gettable_structure` - true if the _latest_ scrape was able to get the structure details
* `last_structure_get` - when structure details were last updated
* `is_public_structure` - true if the _latest_ scrape saw it on the public structure ESI endpoint
* `last_seen_public_structure` - when the structure was last seen as public
* `is_market_structure` - true if the _latest_ scrape saw it on the structure market ESI endpoint
* `last_seen_market_structure` - when the structure was last seen with a market
* `is_sovereignty_structure` - true if the _latest_ scrape saw it on the sovereignty ESI endpoint
* `last_seen_sovereignty_structure` - when the structure was last seen as sovereignty

The [latest file](https://data.everef.net/structures/structures-latest.v2.json) is intended to be a list of all _current_ structures.
Whenever the scrape runs, and structure which hasn't been seen on any of the endpoints for 30 days will be removed.
To get structures older than this, please refer to older files.

## Notes

### Backfills

Several backfills were provided by [Sir SmashAlot](https://evewho.com/character/178497468)
and [Ethan02](https://evewho.com/character/1056136399) of [Adam4Eve](https://www.adam4eve.eu/).
Thank you.
These have been fully incorporated into the first proper scrape here:
[structures-2024-03-31_04-34-43.v2.json.bz2](https://data.everef.net/structures/history/2024/2024-03-31/structures-2024-03-31_04-34-43.v2.json.bz2).
Details are available on [this Github issue](https://github.com/autonomouslogic/eve-ref/issues/2).

The v1 structure scrapes only included the `/universe/structures/` endpoint.
All the IDs from the v1 files have been incorporated into the file above.

Many of the backfilled structures are no longer available to be queried and therefore do not have accurate timestamps.
To show that they were once queryable, placeholder timestamps of `1970-01-01T00:00:00Z` were used.

### Scripting

To get all market structures:
```shell
curl -s https://data.everef.net/structures/structures-latest.v2.json | jq '.[] | select(.is_market_structure)'
```

### Version 1
The version 1 files are simply a lists of public structure IDs.

### Other sources

Adam4Eve maintains a list of structures accessible at [adam4eve.eu/structures.php](https://www.adam4eve.eu/structures.php).
They have a raw list here: [playerStructure_IDs.txt](https://static.adam4eve.eu/IDs/playerStructure_IDs.txt)