# Public Contract Snapshots
[data.everef.net/public-contracts/](https://data.everef.net/public-contracts/) contain full snapshots of all the publicly
available contracts.
Additionally, any dynamic (mutated Abyssal) items have their dogma attributes and effects fetched and included as well.
This scrape runs twice per hour at 0 and 30 minutes past the hour.

The archives contain several CSV files and each should be relatively easy to insert directly into a database.

Produced by the [scrape-public-contracts](../commands/scrape-public-contracts.md) command.

## Format
Each archive contains the following files:

### `meta.json`
A JSON file with the following fields:
* `datasource`: The EVE instance, always "tranquility".
* `scrape_start`: The time the scrape started.
* `scrape_end`: The time the scrape ended.

### `contracts.csv`
All the contract entries from [/contracts/public/{region_id}/](https://esi.evetech.net/ui/#/operations/Contracts/get_contracts_public_region_id):
* `collateral`
* `contract_id`
* `date_expired`
* `date_issued`
* `days_to_complete`
* `end_location_id`
* `issuer_corporation_id`
* `issuer_id`
* `price`
* `reward`
* `start_location_id`
* `title`
* `type`
* `volume`
* `for_corporation`
* `buyout`

EVE Ref adds the following columns:
* `http_last_modified`
* `region_id`
* `station_id`
* `system_id`
* `constellation_id`

### `contract_non_dynamic_items.csv`
_Not used._

### `contract_items.csv`
All the item entries from [/contracts/public/items/{contract_id}/](https://esi.evetech.net/ui/#/operations/Contracts/get_contracts_public_items_contract_id):

* `is_blueprint_copy`
* `is_included`
* `item_id`
* `material_efficiency`
* `quantity`
* `record_id`
* `runs`
* `time_efficiency`
* `type_id`

EVE Ref adds the following columns:
* `http_last_modified`
* `contract_id`

### `contract_dynamic_items.csv`
All the dynamic item entries from [/dogma/dynamic/items/{type_id}/{item_id}/](https://esi.evetech.net/ui/#/operations/Dogma/get_dogma_dynamic_items_type_id_item_id):

* `created_by`
* `mutator_type_id`
* `source_type_id`

EVE Ref adds the following columns:
* `item_id`
* `contract_id`
* `http_last_modified`

### `contract_dynamic_items_dogma_attributes.csv`
All the dynamic item attribute entries from `dogma_attributes` on [/dogma/dynamic/items/{type_id}/{item_id}/](https://esi.evetech.net/ui/#/operations/Dogma/get_dogma_dynamic_items_type_id_item_id):
* `attribute_id`
* `value`

EVE Ref adds the following columns:
* `contract_id`
* `item_id`
* `http_last_modified`

### `contract_dynamic_items_dogma_effects.csv`
All the dynamic item effects entries from `dogma_effects` on [/dogma/dynamic/items/{type_id}/{item_id}/](https://esi.evetech.net/ui/#/operations/Dogma/get_dogma_dynamic_items_type_id_item_id):
* `effect_id`
* `is_default`

EVE Ref adds the following columns:
* `contract_id`
* `item_id`
* `http_last_modified`

### `contract_bids.csv`
All the bid entries from [/contracts/public/bids/{contract_id}/](https://esi.evetech.net/ui/#/operations/Contracts/get_contracts_public_bids_contract_id):

* `amount`
* `bid_id`
* `date_bid`

EVE Ref adds the following columns:
* `http_last_modified`
* `contract_id`

