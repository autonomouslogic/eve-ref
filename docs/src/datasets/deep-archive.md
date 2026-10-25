# Deep Archive
The [Deep Archive](https://data.everef.net/deep-archive) moves old data off
[data.everef.net](https://data.everef.net/) and into torrent archives.

ℹ️ **To help seed it**, add [the RSS feed](https://data.everef.net/deep-archive/everef-deep-archive.xml) to your torrent client.

## Structure
Each archive is one torrent file, named `everef-eve-online-deep-archive-NN.torrent`, where `NN` is a sequential number.
The files are laid out exactly as they were originally on `data.everef.net`.

Alongside the torrent files are text files named `everef-eve-online-deep-archive-NN.txt`.
These simply contain a list of all the files in each torrent file and can be used to search for specific content.

Alongside the torrent files are [minisign](https://jedisct1.github.io/minisign/) signatures of the torrent
files themselves, named `everef-eve-online-deep-archive-NN.torrent.minisig`.

## Signature Verification
There are two things to verify: the `.torrent` file itself and the individual files inside the torrent after downloading.
Torrent files have their own hashes, which guards against corruption.
The signatures can be used to verify that the data did indeed come from EVE Ref originally and has not been tampered with.

The public key for both is published at
[everef.net/.well-known/minisign.pub](https://everef.net/.well-known/minisign.pub).

### Verifying the `.torrent` file
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

### Verifying the downloaded files
Once the torrent has downloaded, the SHA-256 checksum file `everef-eve-online-deep-archive-NN.sha256` and signature file
`everef-eve-online-deep-archive-NN.sha256.minisig` can be used to double-check the content.
See the readme at `everef-eve-online-deep-archive-NN-README.md` for instructions.

## What gets archived
Files that have been on the site for at least 2 years, oldest first.
Once a torrent is published, those files are removed from `data.everef.net`.
The torrent and its seeders become the only remaining source.

Archiving is done on an ad-hoc basis without a set schedule.
Whenever a new archive has been prepared, it will be added to [everef-deep-archive.xml](https://data.everef.net/deep-archive/everef-deep-archive.xml).


### What _never_ gets archived
* [`market-history/`](market-history.md) and [`killmails/`](killmails.md), since these effectively function as a database, with old files updated in-place.
* [`ccp/`](https://data.everef.net/ccp/), CCP's own data (SDE, MER, image collections and so on), for now.* Any `*-latest*` files

## Published Torrents
Below is a list of published torrents:

| Torrent | v1 info-hash | v2 info-hash | Magnet |
|---|---|---|---|
