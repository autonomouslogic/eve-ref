# EVE Ref EVE Online Deep Archive #{sequence}
This is an EVE Ref EVE Online Deep Archive torrent dated up to {cutoff}.
It contains data from [data.everef.net](https://data.everef.net/).
For more information, see [the docs](https://docs.everef.net/datasets/deep-archive.html).

## Verifying the files
Besides the torrent's own hashes, a SHA-256 checksum file is included at `{id}.sha256`, and a
[minisign](https://jedisct1.github.io/minisign/) signature at `{id}.sha256.minisig`.

To verify the hash file:
```
minisign -Vm {id}.sha256 -P RWTxrmJ0f1feiHhWrnI/WUEf/18UaRb2aFgif8+y2Q8YR1JGa1pj4juk
sha256sum -c {id}.sha256
```

Or via Docker:
```
docker run --rm -v "$PWD:/data" jedisct1/minisign -Vm /data/{id}.sha256 -P RWTxrmJ0f1feiHhWrnI/WUEf/18UaRb2aFgif8+y2Q8YR1JGa1pj4juk
```

The public key is published at [everef.net/.well-known/minisign.pub](https://everef.net/.well-known/minisign.pub).

## Merging with other EVE Ref Deep Archive torrents
Every EVE Ref Deep Archive torrent lays its files out exactly as they were on
[data.everef.net](https://data.everef.net/), and no path is ever repeated across two torrents. That means every
torrent in this series can be downloaded into the same folder, with no subfolder per torrent, and the files all
fit together into one tree matching the site: in qBittorrent, choose "Don't create subfolder" when adding the
torrent (or move a `{id}/` folder's contents up a level for a client without that option). This is how EVE Ref
itself keeps its own copy of everything archived so far.

## Index snapshot
`{id}-index.tar.xz` is a snapshot of `index.json` from every data.everef.net folder this torrent archives a file
from, taken when the torrent was built. Those files describe the folder as it was on data.everef.net (sizes, upload
times, and the file type, date and sequence number data.everef.net worked out from each name) but are rewritten on
every run of the site's own indexer, so they aren't selected into the torrent itself; this tarball is the copy that
doesn't change. Its members are named by their original site paths, so extracting it with `tar xJf {id}-index.tar.xz` inside a
copy of the site's root puts each `index.json` back where it was. `{id}.sha256` covers it too.

## Licence
EVE Online and the EVE logo are the registered trademarks of CCP hf. All rights are reserved worldwide.
All other trademarks are the property of their respective owners. EVE Online, the EVE logo, EVE and all
associated logos and designs are the intellectual property of CCP hf. All artwork, screenshots,
characters, vehicles, storylines, world facts or other recognizable features of the intellectual property
relating to these trademarks are likewise the intellectual property of CCP hf. CCP hf. has granted
permission to EVE Ref to use EVE Online and all associated logos and designs for promotional and
information purposes on its website but does not endorse, and is not in any way affiliated with, EVE Ref.
CCP is in no way responsible for the content on or functioning of this website, nor can it be liable for
any damage arising from the use of this website.
