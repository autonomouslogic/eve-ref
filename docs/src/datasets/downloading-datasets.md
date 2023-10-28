# Downloading Datasets
Downloading full datasets off data.everef.net is allowed.
The current host does not charge for bandwidth, so knock yourself out.

## Conditional updates
When re-downloading existing datasets for updates, use the `etag` and `content-length` headers where possible,
and avoid the `last-modified` header altogether.
This is a limitation of the current host where the `last-modified` header will always be the current time.
I'm trying to get them to fix it, but so far they haven't.

## Using wget
To download all the data, you can use [`wget`](https://linux.die.net/man/1/wget).
The only real problem with `wget` is that it does not support multiple concurrent downloads.

For instance, to download the [market orders](market-orders.md) for a particular year, you can use the following command:

```bash
wget -r -np -nc -nv --domains=data.everef.net -R index.html https://data.everef.net/market-orders/history/2023/
```

* `-r` enables recursive retrieval, where `wget` will examine the HTML and resolve relative links.
* `-np` prevents ascending to the parent directory when retrieving recursively.
* `-nc` prevents downloading files that already exist locally.
* `-nv` prevents printing of the download progress.
* `--domains=data.everef.net` restricts the download to the `data.everef.net` domain.
* `-R index.html` prevents downloading of the `index.html` files.
* _Do not use_ `-N` to enforce timestamp checking. The current host doesn't handle the `Last-Modified` header properly and this will result in all the files being redownloaded on every run.

## Using wget on Docker

If you don't have `wget` installed, you can use the [`mwendler/wget`](https://hub.docker.com/r/mwendler/wget) Docker image.
This image hasn't been updated in a while, but it still works.
```bash
docker run --rm -v $(pwd):/data -w /data mwendler/wget -r -np -nc -nv --domains=data.everef.net -R index.html --no-check-certificate https://data.everef.net/market-orders/history/2023/
```