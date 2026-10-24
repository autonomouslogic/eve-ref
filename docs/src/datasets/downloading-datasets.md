# Downloading Datasets
Downloading full datasets off [data.everef.net](https://data.everef.net/) is allowed.
The current host does not charge for bandwidth, so knock yourself out.

`data.everef.net` does not return `last-modified` or `etag` headers, so conditional requests based on those headers do not work.
Use `content-length`/file size instead to avoid downloading the same data multiple times.

A special header is returned, `x-bz-info-src_last_modified_millis`, which contains the millisecond UNIX timestamp of the true file modification time.
This is available on both HEAD and GET requests.

## Changelog
- **2026-10-20 23:21 UTC**: `data.everef.net` stopped returning `last-modified` and `etag` headers.
  Conditional requests and timestamp-based comparisons no longer work; use size-based
  comparisons instead. Exposed `x-bz-info-src_last_modified_millis`.

## Download with rclone
The preferred method to download the data is with [rclone](https://rclone.org/) as it supports multiple concurrent downloads.

As an example, to download all market orders from 2023 using concurrent downloads, you can use the following command:
```shell
rclone copy -v --checkers 2 --transfers 2 --multi-thread-streams 0 --size-only --http-url https://data.everef.net :http:/market-orders/history/2023 pathtocopyto
```

Use `rclone copy`, not `rclone sync`.
Old data is periodically moved off `data.everef.net` into the [Deep Archive](deep-archive.md), and `sync` would delete your local copies of those files when they disappear from the site.
`copy` only ever adds and updates files.

## Download with rclone on Docker
To download data with `rclone` on Docker instead, use the following command:
```shell
docker run --rm -v $(pwd):/data -w /data rclone/rclone copy -v --checkers 2 --transfers 2 --multi-thread-streams 0 --size-only --http-url https://data.everef.net :http:/market-orders/history/2023 pathtocopyto
```

## Mount with rclone
`rclone` supports [mounting](https://rclone.org/commands/rclone_mount/) straight onto the file system:

```shell
rclone mount --attr-timeout 1m --dir-cache-time 1m --vfs-cache-mode full --checkers 2 --transfers 2 --multi-thread-streams 0 --size-only --http-url https://data.everef.net :http:/ pathtomount
```

## Download with wget
`wget` is not recommended: it does not support multiple concurrent downloads, and it has
no size-only comparison mode. Its `-N` timestamp flag no longer works at all since
`last-modified` is no longer returned, so skipping unchanged files isn't possible with it.

For instance, to download the [market orders](market-orders.md) for a particular year, you can use the following command:

```bash
wget -r -np -nv --domains=data.everef.net -R index.html https://data.everef.net/market-orders/history/2023/
```

* `-r` enables recursive retrieval, where `wget` will examine the HTML and resolve relative links.
* `-np` prevents ascending to the parent directory when retrieving recursively.
* `-nv` prevents printing of the download progress.
* `--domains=data.everef.net` restricts the download to the `data.everef.net` domain.
* `-R index.html` prevents downloading of the `index.html` files.

## Download with wget on Docker

If you don't have `wget` installed, you can use the [`mwendler/wget`](https://hub.docker.com/r/mwendler/wget) Docker image.
This image hasn't been updated in a while, but it still works.
```bash
docker run --rm -v $(pwd):/data -w /data mwendler/wget -r -np -nv --domains=data.everef.net -R index.html --no-check-certificate https://data.everef.net/market-orders/history/2023/
```
