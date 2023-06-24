---
title: Field Replication
---
# Field Replication

The scrape jobs som to mimic the format returned from the ESI as closely as possible.
This means the scrapes are not implemented using the ESI Swagger specification, but rather perform a direct HTTP
request and uses the result as-is.
Therefore it's not required to keep an ESI client up-to-date for the purposes of scraping data and if any new fields
appear, they will be automatically included without any need for code changes.
