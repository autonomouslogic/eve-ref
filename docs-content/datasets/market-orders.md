# Market Order Snapshots

[These files](https://data.everef.net/market-orders/) contain full snapshots of the market orders in all regions.
The scrape runs twice per hour at 15 and 45 minutes past the hour.
The archives are single compressed CSV files and should be relatively easy to insert into a database.

Produced by the [scrape-market-orders](../commands/scrape-market-orders.md) command.

Fuzzwork has a similar [snapshot archive](https://market.fuzzwork.co.uk/api/), taken at 6 and 36 minutes past the hour.
