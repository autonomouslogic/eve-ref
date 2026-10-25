# Deep Archive
The [Deep Archive](https://data.everef.net/deep-archive/) moves old data off
[data.everef.net](https://data.everef.net/) and into torrent archives.

## Published Torrents
Below is a list of published torrents.

ℹ️ **To help seed it**, add [the RSS feed](https://data.everef.net/deep-archive/everef-eve-online-deep-archive.xml) to
your torrent client and configure it for automatic downloads.

| Torrent                                                                                                                     | Files                                                                                                               | Size     | Magnet link                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | v1 infohash                                | v2 infohash                                                        |
|-----------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------|--------------------------------------------------------------------|
| [everef-eve-online-deep-archive-00.torrent](https://data.everef.net/deep-archive/everef-eve-online-deep-archive-00.torrent) | [everef-eve-online-deep-archive-00.txt](https://data.everef.net/deep-archive/everef-eve-online-deep-archive-00.txt) | 10.2 GiB | [Magnet link](magnet:?xt=urn:btih:490948968a99044b47b4745d6669f17615395fa0&xt=urn:btmh:122064d67e474d13c31da81afecb310bac2082dd416ea0f0f61b90e55da6c89991a6&dn=everef-eve-online-deep-archive-00&tr=udp://tracker.opentrackr.org:1337/announce&tr=udp://open.stealth.si:80/announce&tr=udp://open.demonii.com:1337/announce&tr=udp://open.tracker.cl:1337/announce&tr=udp://exodus.desync.com:6969/announce&tr=udp://tracker.skynetcloud.site:6969/announce&tr=udp://tracker.qu.ax:6969/announce&tr=udp://tracker.corpscorp.online:80/announce&tr=udp://tracker.bittor.pw:1337/announce&tr=udp://tracker.tryhackx.org:6969/announce) | `490948968a99044b47b4745d6669f17615395fa0` | `64d67e474d13c31da81afecb310bac2082dd416ea0f0f61b90e55da6c89991a6` |

To download the archives without a torrent client, use [aria2](https://aria2.github.io/):
```bash
aria2c https://data.everef.net/deep-archive/everef-eve-online-deep-archive-00.torrent
```

## Structure
Each archive is one torrent file, named `everef-eve-online-deep-archive-NN.torrent`, where `NN` is a sequential number.
The files are laid out exactly as they were originally on `data.everef.net`.

Text files named `everef-eve-online-deep-archive-NN.txt` contain  a list of all the files in each torrent file and can
be used to search for specific content.

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
Whenever a new archive has been prepared, it will be added to [everef-eve-online-deep-archive.xml](https://data.everef.net/deep-archive/everef-eve-online-deep-archive.xml).


### What _never_ gets archived
* [`market-history/`](market-history.md) and [`killmails/`](killmails.md), since these effectively function as a database, with old files updated in-place.
* [`ccp/`](https://data.everef.net/ccp/), CCP's own data (SDE, MER, image collections and so on), for now.* Any `*-latest*` files
