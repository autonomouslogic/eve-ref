# EVE Ref Deep Archive

The [Deep Archive](https://data.everef.net/deep-archive) moves old data off-site and into torrent archives.
Hosting all EVE Online data on [data.everef.net](https://data.everef.net/) forever isn't sustainable.

**To help host it**, add [the RSS feed](https://data.everef.net/deep-archive/everef-deep-archive.xml) to your torrent client.

## Structure
Each archive is one torrent file, named `everef-deep-archive-YYYY-MM-DD.torrent`.
Generally, they hold data from before the specified date, but that's not a hard requirement.
The files are laid out exactly as it was on originally on data.everef.net.

Alongside the torrent files are TXT files names `everef-deep-archive-YYYY-MM-DD.txt`.
These simply contain a list of all the files in each torrent file
They can be used to search for specific data.

### What gets archived
Files that have been on the site for at least 2 years, oldest first.
Once a torrent is published, those files are removed from data.everef.net.
The torrent and its seeders become their only remaining source.

### What _never_ gets archived
* [`market-history/`](market-history.md) and [`killmails/`](killmails.md),
  since these effective function as a database, with past files updated in place
* `ccp/`, CCP/FC's own reference data
* Any `*-latest` files

## The RSS Feed
Archiving is done on an ad-hoc basis.
Whenever a new archive has been prepared, it will be added to [everef-deep-archive.xml](https://data.everef.net/deep-archive/everef-deep-archive.xml).
To make sure you have a copy of all the data, simply add that feed to your BitTorrent client to sync it automatically. 
