# History Rollups

To keep the number of files on [data.everef.net](https://data.everef.net/) down,
finished years of some datasets are rolled up into a single archive per year.
The year's directory is removed and replaced by one `.tar.xz` file next to it:

```
<dataset>/history/<year>/                  (removed)
<dataset>/history/<dataset>-<year>.tar.xz  (added)
```

For example, [`incursions/history/incursions-2023.tar.xz`](https://data.everef.net/incursions/history/incursions-2023.tar.xz).

Only finished years are rolled up. The current year always stays as individual files.
Market history and killmails will never be rolled up.

## Contents

* Paths match the data site: `<dataset>/history/<year>/<YYYY-MM-DD>/<file>.json`.
  Extracting several archives into the same directory recreates the original layout.
* Data files are plain JSON. The `.bz2` compression is removed, along with the `.bz2` suffix.
* Each file's modification time is its original upload time.
* The [`index.json`](index.md#json-index-format) files are included. Their entries still refer to the original `.json.bz2` names.
* `index.html` files are not included.

## Extracting

```shell
tar -xJf incursions-2023.tar.xz
```

## Older archives

Some archives predate this format:

* `incursions/history/incursions-2022.tar.xz`
* `industry-systems/history/industry-systems-2017.tar.bz2` to `industry-systems-2022.tar.bz2` (no 2020 archive)
* `sovereignty-campaigns/history/sovereignty-campaigns-2017.tar.bz2` to `sovereignty-campaigns-2022.tar.bz2` (no 2020 archive)
* `sovereignty-map/history/sovereignty-map-2017.tar.bz2` to `sovereignty-map-2022.tar.bz2` (no 2020 archive)
* `sovereignty-structures/history/sovereignty-structures-2017.tar.bz2` to `sovereignty-structures-2022.tar.bz2` (no 2020 archive)
* `system-jumps/history/system-jumps-2017.tar.bz2` to `system-jumps-2022.tar.bz2` (no 2020 archive)
* `system-kills/history/system-kills-2017.tar.bz2` to `system-kills-2022.tar.bz2` (no 2020 archive)
* `wars/history/wars-2003.tar.bz2` to `wars-2020.tar.bz2`
