# Import Market History

*While this command has been tested and works, it should still be considered to be **in development**.
Make sure you run this command against its own logical database to avoid any risk to your production data.
Follow [this milestone on Github](https://github.com/autonomouslogic/eve-ref/milestone/5) to see what issues are outstanding.*

Scraping the ESI market history endpoint is a slow process and it's hard to get a complete dataset.
EVE Ref runs a daily [market history scrape](scrape-market-history.md), which tries to gather as much complete data as possible.
The scrape job runs in batches with multiple updates throughout the day.
This import job will only import new data that not already in your database, meaning this import can be scheduled to essentially piggy-back off EVE Ref's scrape job.

The import runs in Docker, so should be easy to run on any operating system.

## Process
1. EVE Ref includes a [Flyway](https://flywaydb.org/) setup to create and upgrade its own tables. Unless disabled, this is automatically run at the beginning of the import.
   * **Caution**: it is highly recommended you isolate the import in its own logical database and user/password to avoid conflicts. 
2. Your database is queried for the number of unique region-type pairs present for each day. If you omit `IMPORT_MARKET_HISTORY_MIN_DATE`, this means your entire database will be queried every time.
3. For each day your database doesn't contain any pairs, market history will be downloaded and inserted.
4. For each day your database contains fewer pairs than what's available, market history with be downloaded and inserted.
5. Each day is inserted inside a SQL transaction.

## Basic Usage
```shell
docker run -it --rm \
	-e "DATABASE_URL=jdbc:postgresql://localhost:5432/everef" \
	-e "DATABASE_USERNAME=everef" \
	-e "DATABASE_PASSWORD=password1" \
	-e "IMPORT_MARKET_HISTORY_MIN_DATE=2022-01-01" \
	-e "INSERT_SIZE=100000" \
	-e "FLYWAY_AUTO_MIGRATE=true" \
	autonomouslogic/eve-ref:latest \
	import-market-history
```

* `DATABASE_URL` - the JDBC URL for the database.
* `DATABASE_USERNAME` - the username to use when connecting to the database.
* `DATABASE_PASSWORD` - the password to use when connecting to the database.
* `IMPORT_MARKET_HISTORY_MIN_DATE` - how far back to import data for. Defaults to `2003-01-01`.
* `INSERT_SIZE` - how many records to insert with each `INSERT` statement. Defaults to `100`.
* `FLYWAY_AUTO_MIGRATE` - whether to run Flyway migrations before importing data. Defaults to `true`.

### Scheduling
The market history scrape job runs daily, but uploads data in batches, roughly every 30 minutes.
If you want to piggy-back off this scrape, you can schedule this import job to run however often you wish.
Only days with new data available will be imported.

### Min Date
You can omit `IMPORT_MARKET_HISTORY_MIN_DATE` entirely on the first run to import all data since 2003.
For subsequent runs, it's recommended you specify it.
Otherwise, the import will keep checking your database for data since 2003 when no new updates are available anyway.
The ESI market history endpoint returns data from 400 or more days ago, so any of those files could be updated at any point.
On a Linux system, you can set the date to 500 days ago to ensure you're always up-to-date with the latest:
```shell
-e "IMPORT_MARKET_HISTORY_MIN_DATE=$(date -u --date="500 days ago" +"%Y-%m-%d")"
```

### Database Table
You can disable Flyway with `FLYWAY_AUTO_MIGRATE=false`, you can create the table manually.

```sql
create table market_history
(
    date               date           not null,
    region_id          integer        not null,
    type_id            integer        not null,
    average            numeric(20, 2) not null,
    highest            numeric(20, 2) not null,
    lowest             numeric(20, 2) not null,
    volume             bigint         not null,
    order_count        integer        not null,
    http_last_modified timestamp with time zone,
    primary key (date, region_id, type_id)
);

create index market_history_region_id_type_id
    on market_history (region_id, type_id);

create index market_history_type_id
    on market_history (type_id);


```

## Supported Databases
* PostgreSQL - supported
* H2 - supported
* MySQL - [planned](https://github.com/autonomouslogic/eve-ref/issues/369)
* MS SQL - [planned](https://github.com/autonomouslogic/eve-ref/issues/370)
