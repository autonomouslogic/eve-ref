# Scrape Market History

Produces the [market history dataset](../datasets/market-history.md).

This job runs daily after downtime and takes almost all day to complete.
This is due to the ESI market history endpoint being rate limited to 300 requests per minute (5 per second)
and that each region-type pair has to be requested individually.
There are roughly 1.7 million valid region-type pairs to try, with the crawler trying about 380k each day.
It will prioritise more important region-type pairs to be fetched first, but it's essentially guessing what data to fetch.
See [Sources](#sources) below for how it does that.

In order for the crawler to be resilient to crashes and be able to resume where it left off,
it processes these region-type pairs in batches.
Each batch consists of 9,000 pairs.
Once a batch has been scanned, any new daily files are uploaded to the archive.
Each batch takes roughly 30 minutes to complete, meaning the first batch of data is available soon after downtime,
and any consumers don't have to wait all day.

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

### 2023-09-26 and 2023-10-01
On 2023-09-26:
```
HistoryRegionTypeSource returned 248551 pairs, adding 248551 new pairs, new total: 248551, 75 new regions
ActiveOrdersRegionTypeSource returned 218571 pairs, adding 33043 new pairs, new total: 281594, 0 new regions
RecentRegionTypeRemover returned 0 pairs, adding 0 new pairs, new total: 281594, 0 new regions
```

And 2023-10-01:
```
HistoryRegionTypeSource returned 248333 pairs, adding 248333 new pairs, new total: 248333, 75 new regions
ActiveOrdersRegionTypeSource returned 218219 pairs, adding 33272 new pairs, new total: 281605, 0 new regions
RecentRegionTypeRemover returned 0 pairs, adding 0 new pairs, new total: 281605, 0 new regions
```

Both show almost the same numbers for `HistoryRegionTypeSource`:

### 2023-10-02
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
This is a 0.5% increase, meaning the new sources only had a small impact.
However, in the interest of collecting all the available data, any impact is good.
Due to a bug in the new stat reporting added at the end of the run, it's unknown which new sources contributed the most. 

### 2023-10-04
A few days later with some changes made and the stats fixed running [2.32.0](https://github.com/autonomouslogic/eve-ref/releases/tag/2.32.0):

```
HistoryRegionTypeSource returned 250344 pairs, adding 250344 new pairs, new total is 250344, 76 new regions, est. runtime PT13H54M28S
ActiveOrdersRegionTypeSource returned 218787 pairs, adding 33292 new pairs, new total is 283636, 3 new regions, est. runtime PT1H50M58S
HistoricalOrdersRegionTypeSource returned 183288 pairs, adding 4025 new pairs, new total is 287661, 1 new regions, est. runtime PT13M25S
TopTradedRegionTypeSource returned 35477 pairs, adding 35477 new pairs, new total is 323138, 0 new regions, est. runtime PT1H58M15S
ExplorerRegionTypeSource returned 17297 pairs, adding 14061 new pairs, new total is 337199, 22 new regions, est. runtime PT46M52S
RecentRegionTypeRemover returned 0 pairs, adding 0 new pairs, new total is 337199, 0 new regions, est. runtime PT0S
```

The number of pairs went up a bit (250130 to 250344) as did the number of regions (75 to 76),
meaning the history now contains data for one more.
Active orders now also scans all regions and this time it's returning 3 new ones.
Stats for the end of the run:

```
Source HistoryRegionTypeSource: hit 240631 of 250344 pairs - 96.1%
Source ActiveOrdersRegionTypeSource: hit 286 of 33292 pairs - 0.9%
Source HistoricalOrdersRegionTypeSource: hit 45 of 4025 pairs - 1.1%
Source TopTradedRegionTypeSource: hit 1515 of 35477 pairs - 4.3%
Source ExplorerRegionTypeSource: hit 149 of 14061 pairs - 1.1%
```

### 2023-10-06

```
HistoryRegionTypeSource returned 251934 pairs, adding 251934 new pairs, new total is 251934, 78 new regions, est. runtime PT13H59M46S
ActiveOrdersRegionTypeSource returned 219465 pairs, adding 33809 new pairs, new total is 285743, 1 new regions, est. runtime PT1H52M41S
HistoricalOrdersRegionTypeSource returned 183614 pairs, adding 3923 new pairs, new total is 289666, 1 new regions, est. runtime PT13M4S
TopTradedRegionTypeSource returned 35499 pairs, adding 35499 new pairs, new total is 325165, 0 new regions, est. runtime PT1H58M19S
ExplorerRegionTypeSource returned 17141 pairs, adding 13920 new pairs, new total is 339085, 22 new regions, est. runtime PT46M24S
RecentRegionTypeRemover returned 0 pairs, adding 0 new pairs, new total is 339085, 0 new regions, est. runtime PT0S
```

251934 pairs are now present in the history, adding 3383 and 3 new regions since before the new sources were added.

```
Source HistoryRegionTypeSource: hit 242656 of 251934 pairs - 96.3%
Source ActiveOrdersRegionTypeSource: hit 363 of 33809 pairs - 1.1%
Source HistoricalOrdersRegionTypeSource: hit 43 of 3923 pairs - 1.1%
Source TopTradedRegionTypeSource: hit 85 of 35499 pairs - 0.2%
Source ExplorerRegionTypeSource: hit 161 of 13920 pairs - 1.2%
```

Much less impact from `TopTradedRegionTypeSource` than before, with the others seeing similar hits.
This is probably because once top-traded has a hit, it goes into the history and will be picked up on the next run.
At the end of this run, 252484 pairs and 79 regions were in the history, meaning 550 pairs and 1 region were discovered.

### 2024-05-02

214 days since initial update for two full rounds of exploration.


```
HistoryRegionTypeSource returned 277699 pairs, adding 277699 new pairs, new total is 277699, 85 new regions, est. runtime PT15H25M39S
ActiveOrdersRegionTypeSource returned 233649 pairs, adding 37114 new pairs, new total is 314813, 1 new regions, est. runtime PT2H3M42S
HistoricalOrdersRegionTypeSource returned 194129 pairs, adding 4944 new pairs, new total is 319757, 0 new regions, est. runtime PT16M28S
TopTradedRegionTypeSource returned 35532 pairs, adding 35532 new pairs, new total is 355289, 0 new regions, est. runtime PT1H58M26S
ExplorerRegionTypeSource returned 17289 pairs, adding 13893 new pairs, new total is 369182, 16 new regions, est. runtime PT46M18S
RecentRegionTypeRemover returned 0 pairs, adding 0 new pairs, new total is 369182, 0 new regions, est. runtime PT0S
```

277,699 pairs were present before this run, meaning a full 29,148 pairs and 10 regions added since the beginning.

```
Source HistoryRegionTypeSource: hit 267274 of 277699 pairs - 96.2%
Source ActiveOrdersRegionTypeSource: hit 335 of 37114 pairs - 0.9%
Source HistoricalOrdersRegionTypeSource: hit 29 of 4944 pairs - 0.6%
Source TopTradedRegionTypeSource: hit 11 of 35532 pairs - 0.0%
Source ExplorerRegionTypeSource: hit 14 of 13893 pairs - 0.1%
```

Active orders still sees a lot of hits, which makes sense since it's a view into the active market.
Explorer has dropped from 1.2% to 0.1%, which means that strategy is working.
