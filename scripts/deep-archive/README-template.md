# EVE Ref Deep Archive #{id} for EVE Online
This is an EVE Ref Deep Archive torrent dated up to {cutoff}.
It contains data from [data.everef.net](https://data.everef.net/).
For more information, see [the docs](https://docs.everef.net/datasets/deep-archive.html).

## Verifying the files
Besides the torrent's own hashes, a SHA-256 checksum file is included at `{id}.sha256`, and a
[minisign](https://jedisct1.github.io/minisign/) signature at`{id}.sha256.minisig`.

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
