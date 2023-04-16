# Ref Data

The ref data build is currently in development: https://github.com/autonomouslogic/eve-ref/issues/11

## API Paths
* `/dogma/attributes` - array of all attribute IDs - SDE, ESI
* `/dogma/attributes/<attribute_id>` - SDE, ESI
* `/dogma/effects` - array of all effect IDs - SDE, ESI
* `/dogma/effects/<effect_id>` - SDE, ESI
* `/types` - array of all type IDs - SDE, ESI
* `/types/<type_id>` - SDE, ESI
* `/types/<type_id>/dogma/attributes` - SDE, ESI
* `/types/<type_id>/dogma/effects` - SDE, ESI
* `/types/<type_id>/masteries` - SDE
* `/types/<type_id>/variations` - derived
* `/types/<type_id>/traits`
* `/types/<type_id>/traits`

## File paths
The paths in the file can take one of two forms:

1. A condensed form where for instance all type information is containes in a `types.json` with an object keyed by type ID.
    * It's unclear how sub-objects, like dogma attributes for a type, would be stored.
2. A sparse form which is exactly like the API, where for instance each type has its own separate `<type_id>.json` file and `types.json` would be an array of type IDs.

## Data structure

Field names will be `snake_case`, since that's how the ESI does it and all other data on EVE Ref Data comes from there.
It makes sense to continue that format.
The JSON layout will be structured in a way mostly inspired by the ESI, though that may not always be possible.
