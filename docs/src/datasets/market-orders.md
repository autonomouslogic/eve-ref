# Market Order Snapshots

[data.everef.net/market-orders/](https://data.everef.net/market-orders/) contain full snapshots of the market orders in all regions.
The scrape runs twice per hour at 15 and 45 minutes past the hour.
The archives are single compressed CSV files with headers and should be relatively easy to insert into a database.

Produced by the [scrape-market-orders](../commands/scrape-market-orders.md) command.

Fuzzwork has a similar [snapshot archive](https://market.fuzzwork.co.uk/api/) ([mirror](fuzzwork-ordersets.md)), taken at 6 and 36 minutes past the hour.

## Format

The CSV files contain the following columns, which are pulled verbatim from the ESI endpoint:
* `duration`
* `is_buy_order`
* `issued`
* `location_id`
* `min_volume`
* `order_id`
* `price`
* `range`
* `system_id`
* `type_id`
* `volume_remain`
* `volume_total`

Additionally, EVE Ref adds the following columns:
* `http_last_modified`
* `region_id`
* `station_id`
* `constellation_id`
