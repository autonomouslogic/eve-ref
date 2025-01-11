# Killmails

[data.everef.net/killmails/](https://data.everef.net/killmails/) contains archives of all killmails from [zKillboard](https://zkillboard.com/).

Each file in the archive contains all the killmails for that day.
Each killmail is stored in its own file within the archive and is a verbatim copy as it appears on the
[ESI's](https://esi.evetech.net/ui/) `/killmails/{killmail_id}/{killmail_hash}/` endpoint.
Be aware that files are updated in-place when new killmails are added for any given day.

The file [totals.json](https://data.everef.net/killmails/totals.json) contains a count of all the killmails stored per day.
Since killmails are only ever added, this file can be used to determine which days have new killmails.
This file is akin to zKillboard's own [totals.json](https://zkillboard.com/api/history/totals.json) file.

When scraping killmail data, EVE Ref doesn't get the killmails from zKillboard directly, but from the ESI.
This means that any extra information on zKillboard's API isn't included in the files, because we're not scraping that API.
EVE Ref uses zKillboard solely for killmail ID and hashes, which are then used to fetch directly from the source, which is the ESI.
