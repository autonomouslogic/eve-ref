# Scrape Market History

Produces the [market history dataset](../datasets/market-history.md).

This job runs daily after downtime and takes about 17 hours to complete.
This is due mainly to how the ESI market history endpoint is designed where you have to specify both a region and a type,
forcing the crawler to essentially guess what might be available.
See [Sources](#sources) below for more how does that.
There are roughly 1.7 million valid region-type pairs to try, with the crawler trying about 300k each day.
The reason this takes so long is that the ESI endpoint is rate-limited to 300 requests per minute (5 per second).

In order for the crawler to be resilient to crashes and be able to resume where it left off,
it processes these region-type pairs in batches.
Each batch consists of 10,000 pairs.
Once a batch has been scanned, any new daily files are uploaded to the archive.
Each batch takes roughly 30 minutes to complete, meaning the first batch of data is available soon after downtime,
and any consumers don't have to wait 17 hours.

## Sources
There are about 1.7 million valid region-type pairs to search though, which isn't feasible to do daily.
Instead, the following "sources" are implemented to try and intelligently explore the space:

* `HistoryRegionTypeSource` looks at the market history for the past 450 days. Any region-type pairs present in the history at any point will be tried again. These pairs are checked in market cap order, so highly traded items are checked first.
* `ActiveOrdersRegionTypeSource` queries the [active types market endpoint](https://esi.evetech.net/ui/#/operations/Market/get_markets_region_id_types) for each region.
* `HistoricalOrdersRegionTypeSource` looks at [market order snapshots](../datasets/market-orders.md) for the past 30 days. It downloads a random file for each day. This has a similar effect as active types, but looking into the past. 
* `TopTradedRegionTypeSource` also looks at market history for the past 450 days, but identifies the top market cap types and ensures those types are checked in every region.
* `ExplorerRegionTypeSource` takes all 1.7 million pairs and groups them into 100 buckets, trying a different bucket each day. This guarantees that we're exploring the entire space at regular intervals.

## Additional sources on 2023-10-02
The run on 2023-10-02 saw the introduction of `HistoricalOrdersRegionTypeSource`, `TopTradedRegionTypeSource`, and `ExplorerRegionTypeSource`.
This was in release [2.31.0](https://github.com/autonomouslogic/eve-ref/releases/tag/2.31.0).

Before these, the following sources were used (2023-10-01):
```
HistoryRegionTypeSource returned 248333 pairs, adding 248333 new pairs, new total: 248333, 75 new regions
ActiveOrdersRegionTypeSource returned 218219 pairs, adding 33272 new pairs, new total: 281605, 0 new regions
RecentRegionTypeRemover returned 0 pairs, adding 0 new pairs, new total: 281605, 0 new regions
```

An even earlier run (2023-09-26) shows almost the same numbers for `HistoryRegionTypeSource`:
```
HistoryRegionTypeSource returned 248551 pairs, adding 248551 new pairs, new total: 248551, 75 new regions
ActiveOrdersRegionTypeSource returned 218571 pairs, adding 33043 new pairs, new total: 281594, 0 new regions
RecentRegionTypeRemover returned 0 pairs, adding 0 new pairs, new total: 281594, 0 new regions
```

The introduction of the additional sources expanded the search space:
```
HistoryRegionTypeSource returned 248549 pairs, adding 248549 new pairs, new total is 248549, 75 new regions, est. runtime PT13H48M29S
ActiveOrdersRegionTypeSource returned 218291 pairs, adding 33015 new pairs, new total is 281564, 0 new regions, est. runtime PT1H50M3S
HistoricalOrdersRegionTypeSource returned 183595 pairs, adding 4624 new pairs, new total is 286188, 2 new regions, est. runtime PT15M24S
TopTradedRegionTypeSource returned 36000 pairs, adding 15337 new pairs, new total is 301525, 0 new regions, est. runtime PT51M7S
ExplorerRegionTypeSource returned 17085 pairs, adding 14077 new pairs, new total is 315602, 25 new regions, est. runtime PT46M55S
RecentRegionTypeRemover returned 0 pairs, adding 0 new pairs, new total is 315602, 0 new regions, est. runtime PT0S
```

After this run, the history contained 250130 pairs, meaning 1581 new pairs were discovered.
This is a 0.5% increase, meaning the new sources only had a small impact, with a combined hit rate of 2.4%.
However, in the interest of collecting all the available data, any impact is good.
Due to a bug in the new stat reporting added at the end of the run, it's unknown which new sources contributed the most. 
