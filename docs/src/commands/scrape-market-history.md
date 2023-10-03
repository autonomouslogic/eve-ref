# Scrape Market History

Produces the [market history dataset](../datasets/market-history.md).

This job runs daily after downtime and takes about 17 hours to complete.
This is due mainly to how the ESI market history endpoint is designed where you have to specify both a region and a type,
forcing the crawler to essentially guess what might be available.
See [Sources](#sources) below for more how does that.
There are roughly 1.7 million valid region-type pairs to try, with the crawler trying about 320,000 each day.
The reason this takes so long is that the ESI endpoint is rate-limited to 300 requests per minute (5 per second).

In order for the crawler to be resilient to crashes and be able to resume where it left off,
it processes these region-type pairs in batches.
Each batch consists of 10,000 pairs.
Once a batch has been scanned, any new daily files are uploaded to the archive.
Each batch takes roughly 30 minutes to complete, meaning the first batch of data is available soon after downtime,
and any consumers don't have to wait 17 hours.
Please see [downloading datasets](../datasets/downloading-datasets.md) for more information on how to effectively download.

## Sources
There are about 1.7 million valid region-type pairs to search though, which isn't feasible to do daily.
Instead, the following "sources" are implemented to try and intelligently explore the space:

* `HistoryRegionTypeSource` looks at the market history for the past 450 days. Any region-type pairs present in the history at any point will be tried again. These pairs are checked in market cap order, so highly traded items are checked first.
* `ActiveOrdersRegionTypeSource` queries the [active types market endpoint](https://esi.evetech.net/ui/#/operations/Market/get_markets_region_id_types) for each region.
* `HistoricalOrdersRegionTypeSource` looks at [market order snapshots](../datasets/market-orders.md) for the past 30 days. It downloads a random file for each day. This has a similar effect as active types, but looking into the past. 
* `TopTradedRegionTypeSource` also looks at market history for the past 450 days, but identifies the top market cap types and ensures those types are checked in every region.
* `ExplorerRegionTypeSource` takes all 1.7 million pairs and groups them into 100 buckets, trying a different bucket each day. This guarantees that we're exploring the entire space at regular intervals.
