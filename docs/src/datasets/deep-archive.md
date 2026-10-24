# EVE Ref Deep Archive
The [Deep Archive](https://data.everef.net/deep-archive) moves old data off [data.everef.net](https://data.everef.net/) and into torrent archives.

**To help host it**, add [the RSS feed](https://data.everef.net/deep-archive/everef-deep-archive.xml) to your torrent client.

## Structure
Each archive is one torrent file, named `everef-eve-online-deep-archive-YYYY-MM-DD.torrent`.
The date is the cutoff date for the torrent.
It will only contain data from that date or earlier.
However, it may not necesarrily contain all the data from before that date.
The files are laid out exactly as it was originally on `data.everef.net`.

Alongside the torrent files are TXT files names `everef-eve-online-deep-archive-YYYY-MM-DD.txt`.
These simply contain a list of all the files in each torrent file and can be used to quickly search for specific files.

### What gets archived
Files that have been on the site for at least 2 years, oldest first.
Once a torrent is published, those files are removed from `data.everef.net`.
The torrent and its seeders become their only remaining source.

### What _never_ gets archived
* [`market-history/`](market-history.md) and [`killmails/`](killmails.md), since these effective function as a database, with past files updated in-place.
* `ccp/`, CCP/FC's own reference data
* Any `*-latest*` files

## Torrents
Newest first. The [`deep-archive/`](https://data.everef.net/deep-archive/) folder and the RSS feed always have the
complete list.

| Torrent | v1 info-hash | v2 info-hash | Magnet |
|---|---|---|---|

## The RSS Feed
Archiving is done on an ad-hoc basis.
Whenever a new archive has been prepared, it will be added to [everef-deep-archive.xml](https://data.everef.net/deep-archive/everef-deep-archive.xml).
To make sure you have a copy of all the data, add that feed to your BitTorrent client to sync it automatically. 
