# Structures

[data.everef.net/structures/](https://data.everef.net/structures/) contains scrapes of the structures ESI endpoints.

These are:
* `/universe/structures/`
* `/universe/structures/{structure_id}/`
* `/sovereignty/structures/`

Queries are also made to `/markets/structures/{structure_id}/` to determine if there's a market present at the structure.

The scrape file is a single JSON file containing an object with the structure IDs as keys.
The objects themselves are verbatim from the structure endpoint, with the following additional fields:
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

## Scripting

To get all market structures:
```shell
curl -s https://data.everef.net/structures/structures-latest.v2.json | jq '.[] | select(.is_market_structure)'
```

## Backfills

Several backfills were provided by [Sir SmashAlot](https://evewho.com/character/178497468)
and [Ethan02](https://evewho.com/character/1056136399) of [Adam4Eve](https://www.adam4eve.eu/).
Thank you.
These have been fully incorporated into the first proper scrape at **TODO**.
Details are available on the [this Githib issue](https://github.com/autonomouslogic/eve-ref/issues/2).

The older structure scrapes only included the `/universe/structures/` endpoint.
All the scrapes of this endpoint only have also been incorporated into the file above.

Many of the backfilled structures are no longer available to be queried and therefore do not have accurate timestamps.
To show that they were once queryable, placeholder timestamps of `1970-01-01T00:00:00Z` were used.
