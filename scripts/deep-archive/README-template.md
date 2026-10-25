# EVE Ref Deep Archive #{id} for EVE Online
This is an EVE Ref Deep Archive torrent dated up to {cutoff}.
It contains data from [data.everef.net](https://data.everef.net/).
For more information, see [the docs](https://docs.everef.net/datasets/deep-archive.html).

## Verifying the files
Besides the torrent's own hashes, a SHA-256 checksum file included at
[{id}.sha256](https://data.everef.net/deep-archive/{id}.sha256),
signed with [minisign](https://jedisct1.github.io/minisign/) at [{id}.sha256.minisig](https://data.everef.net/deep-archive/{id}.sha256.minisig).

To verify the hash file:
```
minisign -Vm {id}.sha256 -P RWTxrmJ0f1feiHhWrnI/WUEf/18UaRb2aFgif8+y2Q8YR1JGa1pj4juk
sha256sum -c {id}.sha256
```

Or via Docker:

```
docker run --rm -v "$PWD:/data" jedisct1/minisign -Vm /data/{id}.sha256 -P RWTxrmJ0f1feiHhWrnI/WUEf/18UaRb2aFgif8+y2Q8YR1JGa1pj4juk
```

The public key is also published at [everef.net/.well-known/minisign.pub](https://everef.net/.well-known/minisign.pub).

## Licence
EVE Online and the EVE logo are the registered trademarks of CCP hf. All rights are reserved worldwide. CCP hf.
has granted permission to EVE Ref to use EVE Online and associated data for informational purposes; CCP is not
affiliated with, and does not endorse, EVE Ref.
