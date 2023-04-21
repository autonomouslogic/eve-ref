# EVE Online Reference Data

The reference data build is currently in development: https://github.com/autonomouslogic/eve-ref/milestone/3.
Any files [data.everef.net/ref-data](https://data.everef.net/ref-data) are not final and the format may change at any time.

## Motivation
Two primary datasets are available for third-party developers of EVE Online: the SDE and ESI.
While comprehensive, these two are not equal.
There is data in the SDE which isn't in the ESI, and vice-versa.
Additionally, Hoboleaks provides data extracted from the EVE Online client files.

[EVE Ref](https://everef.net/) was originally built on taking all three sources and combining them into one,
making it as comprehensive as possible.
The Reference Data set is an attempt at publishing this data for other developers to consume.

Once built, the dataset will be available in two ways:

* As a static download from [data.everef.net](https://data.everef.net)
* As a REST API

The REST API will be used for EVE Ref itself, as part of the [Nuxt rebuild](https://github.com/autonomouslogic/eve-ref/milestone/2).
For other third-party developers, the REST API will be freely available to consume.

## Data sources

| Data                 | SDE | ESI            | Hoboleaks                    |
|----------------------|-----|----------------|------------------------------|
| Inventory categories | Yes | Yes            |                              |
| Market categories    | Yes | Yes            |                              |
| Inventory types      | Yes | Yes            | Partial - repackaged volumes |
| Dogma attributes     | Yes | Yes            |                              |
| Dogma effects        | Yes | Yes            |                              |
| Planetary schematics | Yes | Yes - no index |                              |
| Blueprints           | Yes | No             |                              |

## Data structure

* Field names will be `snake_case`, since that's how the ESI does it and all other data on EVE Ref Data comes from there.\
  It makes sense to continue that format.
* The JSON layout will be structured in a way mostly inspired by the ESI, though that may not always be possible.
* Prefer keyed objects over arrays - the final object merger should be kept as simple as possible.\
  Since it's not possible to merge arrays in a predictable while preventing data duplication, keyed objects are preferred.
* Names and descriptions will use a language map like the SDE, rather than multiple files like the ESI.
