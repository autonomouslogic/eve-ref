# Market History

[data.everef.net/market-history/](https://data.everef.net/market-history/) contains daily archive files of market history data.
They are produced by the [Scrape Markey History](../commands/scrape-market-history.md) command.

Due to the nature of the ESI market history endpoint, it's possible for new data to be discovered after the daily archive
has been created.
This means that data in the archive may not be complete, but also that archives for past days may be updated in-place.
If you download these archives and insert them into your own database, and want them to stay fully up-to-date,
it's important to be aware of this.

For custom download implementations, [totals.json](https://data.everef.net/market-history/totals.json) contains the total
number of market history records for each day.
Compare this with numbers in your database and update accordingly.
For full downloads of all the files, please see [downloading datasets](downloading-datasets.md).