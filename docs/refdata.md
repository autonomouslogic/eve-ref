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

| Data                            | SDE | ESI             | Hoboleaks         |
|---------------------------------|-----|-----------------|-------------------|
| Accounting Entry Types (?)      |     |                 | Yes               |
| Agent                           | Yes |                 |                   |
| Agent in space (?)              | Yes |                 |                   |
| Agent Types (?)                 |     |                 | Yes               |
| Ancestors                       | Yes | Yes             |                   |
| Asteroid belts                  | Yes | Yes             |                   |
| Bloodlines                      | Yes | Yes             |                   |
| Blueprints                      | Yes |                 |                   |
| Blueprint materials             | Yes |                 |                   |
| Categories                      |     | Yes             |                   |
| Certificates                    | Yes |                 |                   |
| Character attributes            | Yes |                 |                   |
| Clone States (?)                |     |                 | Yes               |
| Compressible Types (?)          |     |                 | Yes               |
| Constellations                  | Yes | Yes             |                   |
| Contraband types (?)            | Yes |                 |                   |
| Control tower resources (?)     | Yes |                 |                   |
| Corportation activities (?)     | Yes |                 |                   |
| Dbuffs (?)                      |     |                 | Yes               |
| Dogma attributes categories     | Yes |                 |                   |
| Dogma attributes                | Yes | Yes             | Localised names   |
| Dogma effects                   | Yes | Yes             |                   |
| Dogma expressions               |     |                 |                   |
| Dogma units                     |     |                 | Unit names        |
| Dynamic Attributes (?)          |     |                 | Yes               |
| Factions                        | Yes | Yes             |                   |
| Graphics                        | Yes | Yes             |                   |
| Graphic Material Sets (?)       |     |                 | Yes               |
| Icons                           | Yes |                 |                   |
| Industry Activities (?)         |     |                 | Yes               |
| Industry Assembly Lines (?)     |     |                 | Yes               |
| Industry Installation Types (?) |     |                 | Yes               |
| Industry Modifier Sources (?)   |     |                 | Yes               |
| Industry Target Filters (?)     |     |                 | Yes               |
| Inventory flags (?)             | Yes |                 |                   |
| Inventory groups                | Yes | Yes             |                   |
| Inventory items (?)             | Yes |                 |                   |
| Inventory names (?)             | Yes |                 |                   |
| Inventory positions (?)         | Yes |                 |                   |
| Inventory types                 | Yes | Yes             | Repackaged volume |
| Inventory unique names          | Yes |                 |                   |
| Landmarks                       | Yes |                 |                   |
| Languages                       | Yes | Yes, indirectly | Yes               |
| Loyalty offers                  |     | Yes             |                   |
| Market groups                   | Yes | Yes             |                   |
| Meta groups                     | Yes |                 |                   |
| Moons                           | Yes | Yes             |                   |
| NPC corporation divisions       | Yes |                 |                   |
| NPC corporation                 | Yes |                 |                   |
| Opportunity groups              |     | Yes             |                   |
| Opportunity tasks               |     | Yes             |                   |
| Planetary schematics            | Yes | Yes - no index  |                   |
| Planets                         | Yes | Yes             |                   |
| Races                           | Yes | Yes             |                   |
| Regions                         | Yes | Yes             |                   |
| Research agents                 | Yes |                 |                   |
| Skins                           | Yes |                 | Yes               |
| Skin licenses                   | Yes |                 |                   |
| Skin materials                  | Yes |                 | Yes               |
| Skin material names             |     |                 | Yes               |
| Stargate                        |     | Yes             |                   |
| Stars                           |     | Yes             |                   |
| Stations                        | Yes | Yes             |                   |
| Station operation               | Yes |                 |                   |
| Station services                | Yes |                 |                   |
| Systems                         | Yes | Yes             |                   |
| Tournament rule sets            | Yes |                 |                   |
| Universes                       | Yes |                 |                   |

## Data structure

* Field names will be `snake_case`, since that's how the ESI does it and all other data on EVE Ref Data comes from there.\
  It makes sense to continue that format.
* The JSON layout will be structured in a way mostly inspired by the ESI, though that may not always be possible.
* Prefer keyed objects to arrays - the final object merger should be kept as simple as possible.\
  Since it's not possible to merge arrays in a predictable while preventing data duplication, keyed objects are preferred.
* Names and descriptions will use a language map like the SDE, rather than multiple files like the ESI.

## Corrections (TBD)

It would be possible to maintain a series of corrections and additions to the data.
For instance, there are Dogma attributes which have no categories and these could be added.
There are also cases where Dogma values are stored "incorrectly". For instance, sometimes the number 10% is stored as `10.0` and other times as `0.1`,
even though the unit for the attribute is "percentage".
These could be corrected.

* Pro: The data is more accurate, consistent, and useful.
* Con: The data isn't a direct copy of the SDE or ESI.

