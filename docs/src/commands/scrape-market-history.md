# Scrape Market History

Produces the market history dataset.

## Sources

There are about 1.7 million valid region-type pairs to search though, which isn't feasible to do daily.
Instead, the following "sources" are implemented to try and intelligently explore the space:

* `HistoryRegionTypeSource` fetches all the pairs that are already in the history, so if anything has previously succeeded (within a range), it will be tried again every day.
* `ActiveOrdersTypeSource` queries the active orders market endpoint for each region.
* `HistoricalOrdersTypeSource` looks at recent market order snapshots.
* `TopTradedTypeSource` looks at which types are most traded and tries for those in all the regions.
* `ExplorerTypeSource` takes all 1.7 million pairs and groups them into buckets, trying a different bucket each day. Currently set so the entire 1.7 million are tried as least once every 100 days.
