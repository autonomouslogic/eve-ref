# Deep Archive
The [Deep Archive](https://data.everef.net/deep-archive) moves old data off [data.everef.net](https://data.everef.net/) and into torrent archives.

ℹ️ **To help host it**, add [the RSS feed](https://data.everef.net/deep-archive/everef-deep-archive.xml) to your torrent client.

## Structure
Each archive is one torrent file, named `everef-eve-online-deep-archive-NN.torrent`, where `NN` is a sequential
two-digit number starting at `00`.
The files are laid out exactly as it was originally on `data.everef.net`.

Each torrent has a cutoff date: it will only contain data from that date or earlier.
However, it may not necessarily contain all the data from before that date.
The cutoff date is stated in the torrent's own `everef-eve-online-deep-archive-NN-README.md`, included inside the
torrent only.

Alongside the torrent files are TXT files named `everef-eve-online-deep-archive-NN.txt`.
These simply contain a list of all the files in each torrent file and can be used to quickly search for specific files.

Also alongside the torrent files are [minisign](https://jedisct1.github.io/minisign/) signatures of the torrent
files themselves, named `everef-eve-online-deep-archive-NN.torrent.minisig`. See [Verification](#verification) below.

## Verification
There are two things to verify: the `.torrent` file itself (before loading it into a client), and, once you have
the torrent, an individual file inside it (useful if you got a copy another way, e.g. off a mirror, without a
torrent client to lean on).

The public key for both is published at
[everef.net/.well-known/minisign.pub](https://everef.net/.well-known/minisign.pub).

### The `.torrent` file
```
curl -O https://data.everef.net/deep-archive/everef-eve-online-deep-archive-NN.torrent
curl -O https://data.everef.net/deep-archive/everef-eve-online-deep-archive-NN.torrent.minisig
curl -O https://everef.net/.well-known/minisign.pub
minisign -V -p minisign.pub -m everef-eve-online-deep-archive-NN.torrent
```

Or via Docker:
```
docker run --rm -v "$PWD:/data" jedisct1/minisign -Vm /data/everef-eve-online-deep-archive-NN.torrent -P RWTxrmJ0f1feiHhWrnI/WUEf/18UaRb2aFgif8+y2Q8YR1JGa1pj4juk
```

### A file inside the torrent
Every torrent includes, at its root, a SHA-256 checksum file (`everef-eve-online-deep-archive-NN.sha256`, standard
`sha256sum` format) and its own minisign signature (`everef-eve-online-deep-archive-NN.sha256.minisig`), the same
way its `everef-eve-online-deep-archive-NN-README.md` does -- see that file for the exact commands.

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
