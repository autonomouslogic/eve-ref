# Downloading Datasets
Downloading full datasets off [data.everef.net](https://data.everef.net/) is allowed.
The current host does not charge for bandwidth, so knock yourself out.

However, use the `last-modified`, `etag`, and `content-length` headers where possible
to avoid downloading the same data multiple times.

## Download with rclone
The preferred method to download the data is with [rclone](https://rclone.org/) as it supports multiple concurrent downloads.

As an example, to sync all market orders from 2023 using concurrent downloads, you can use the following command:
```shell
rclone sync -v --transfers 4 --http-url https://data.everef.net :http:/market-orders/history/2023 pathtosync
```

## Download with rclone on Docker
To sync data with `rclone` on Docker instead, use the following command:
```shell
docker run --rm -v $(pwd):/data -w /data rclone/rclone sync -v --transfers 4 --http-url https://data.everef.net :http:/market-orders/history/2023 pathtosync
```

## Mount with rclone
`rclone` supports [mounting](https://rclone.org/commands/rclone_mount/) straight onto the file system:

```shell
rclone mount --attr-timeout 1m --dir-cache-time 1m --vfs-cache-mode full --transfers 4 --http-url https://data.everef.net :http:/ pathtomount
```

## Download with wget
As an alternative to rclone, you can use [`wget`](https://linux.die.net/man/1/wget).
The only real problem with `wget` is that it does not support multiple concurrent downloads.

For instance, to download the [market orders](market-orders.md) for a particular year, you can use the following command:

```bash
wget -r -np -N -nv --domains=data.everef.net -R index.html https://data.everef.net/market-orders/history/2023/
```

* `-r` enables recursive retrieval, where `wget` will examine the HTML and resolve relative links.
* `-np` prevents ascending to the parent directory when retrieving recursively.
* `-N` enforce timestamp checking to prevent redownloading files that haven't changed.
* `-nv` prevents printing of the download progress.
* `--domains=data.everef.net` restricts the download to the `data.everef.net` domain.
* `-R index.html` prevents downloading of the `index.html` files.

## Download with wget on Docker

If you don't have `wget` installed, you can use the [`mwendler/wget`](https://hub.docker.com/r/mwendler/wget) Docker image.
This image hasn't been updated in a while, but it still works.
```bash
docker run --rm -v $(pwd):/data -w /data mwendler/wget -r -np -N -nv --domains=data.everef.net -R index.html --no-check-certificate https://data.everef.net/market-orders/history/2023/
```
