---
title: Reference Data
---
# Reference Data

The reference data build is currently in development.
<https://github.com/autonomouslogic/eve-ref/milestone/3>.
Any files <https://data.everef.net/reference-data/> are not final and the format may change at any time.

## Spec
The full OpenAPI spec is [available on Github](https://github.com/autonomouslogic/eve-ref/blob/main/spec/reference-data.yaml).

Some example paths:
* <https://ref-data.everef.net/types>
* <https://ref-data.everef.net/types/645>
* <https://ref-data.everef.net/categories>
* <https://ref-data.everef.net/categories/4>
* <https://ref-data.everef.net/groups>
* <https://ref-data.everef.net/groups/18>
* <https://ref-data.everef.net/market_groups>
* <https://ref-data.everef.net/market_groups/1857>
* <https://ref-data.everef.net/meta_groups>
* <https://ref-data.everef.net/meta_group/6>
* <https://ref-data.everef.net/dogma_attributes>
* <https://ref-data.everef.net/dogma_attributes/37>
* <https://ref-data.everef.net/skills>
* <https://ref-data.everef.net/skills/3336>
* <https://ref-data.everef.net/mutaplasmids>
* <https://ref-data.everef.net/mutaplasmids/52225>
* <https://ref-data.everef.net/units>
* <https://ref-data.everef.net/units/1>
* <https://ref-data.everef.net/blueprints>
* <https://ref-data.everef.net/blueprints/999>

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

| Data                            | Reference data      | SDE                                    | ESI                            | Hoboleaks                         |
|---------------------------------|---------------------|----------------------------------------|--------------------------------|-----------------------------------|
| Accounting Entry Types (?)      |                     |                                        |                                | `accountingentrytypes.json`       |
| Agent                           |                     | `fsd/agents.yaml`                      |                                |                                   |
| Agent in space (?)              |                     | `fsd/agentsInSpace.yaml`               |                                |                                   |
| Agent Types (?)                 |                     |                                        |                                | `agenttypes.json`                 |
| Ancestors                       |                     | `fsd/ancestries.yaml`                  | `universe/ancestries.yaml`     |                                   |
| Asteroid belts                  |                     | `fsd/universe`                         | `universe/asteroid_belts.yaml` |                                   |
| Bloodlines                      |                     | `fsd/bloodlines.yaml`                  | `universe/bloodlines.yaml`     |                                   |
| Blueprints                      |                     | `fsd/blueprints.yaml`                  |                                | `blueprints.json`                 |
| Blueprint materials             |                     | `fsd/typeMaterials.yaml`               |                                | `blueprints.json`                 |
| Certificates                    |                     | `fsd/certificates.yaml`                |                                |                                   |
| Character attributes            |                     | `fsd/characterAttributes.yaml`         |                                |                                   |
| Clone States (?)                |                     |                                        |                                | `clonestates.json`                |
| Compressible Types              |                     |                                        |                                | `compressibletypes.json`          |
| Constellations                  |                     | `fsd/universe`                         | `universe/constellations.yaml` |                                   |
| Contraband types (?)            |                     | `fsd/contrabandTypes.yaml`             |                                |                                   |
| Control tower resources (?)     |                     | `fsd/controlTowerResources.yaml`       |                                |                                   |
| Corporatation activities (?)    |                     | `fsd/corporationActivities.yaml`       |                                |                                   |
| Dbuffs (?)                      |                     |                                        |                                | `dbuffs.json`                     |
| Dogma attributes categories     |                     | `fsd/dogmaAttributeCategories.yaml`    |                                |                                   |
| Dogma attributes                | `/dogma_attributes` | `fsd/dogmaAttributes.yaml`             | `dogma/attributes.yaml`        | `localization_dgmattributes.json` |
| Dogma effects                   |                     | `fsd/dogmaEffects.yaml`                | `dogma/effects.yaml`           |                                   |
| Dogma expressions               |                     |                                        |                                |                                   |
| Dogma type attributes           | `/types`            | `fsd/typeDogma.yaml`                   | `universe/types.yaml`          |                                   |
| Dogma type effects              |                     | `fsd/typeDogma.yaml`                   | `universe/types.yaml`          |                                   |
| Dogma units                     |                     |                                        |                                | `dogmaunits.json`                 |
| Dynamic Attributes              | `/mutaplasmids`     |                                        |                                | `dynamicitemattributes.json`      |
| Factions                        |                     | `fsd/factions.yaml`                    | `universe/factions.yaml`       |                                   |
| Graphics                        |                     | `fsd/graphicIDs.yaml`                  | `universe/graphics.yaml`       |                                   |
| Graphic Material Sets (?)       |                     |                                        |                                | `graphicmaterialsets.json`        |
| Icons                           |                     | `fsd/iconIDs.yaml`                     |                                |                                   |
| Industry Activities (?)         |                     |                                        |                                | `industryactivities.json`         |
| Industry Assembly Lines (?)     |                     |                                        |                                | `industryassemblylines.json`      |
| Industry Installation Types (?) |                     |                                        |                                | `industryinstallationtypes.json`  |
| Industry Modifier Sources (?)   |                     |                                        |                                | `industrymodifiersources.json`    |
| Industry Target Filters (?)     |                     |                                        |                                | `industrytargetfilters.json`      |
| Inventory categories            | `/categories`       | `fsd/categoryIDs.yaml`                 | `universe/categories.yaml`     |                                   |
| Inventory flags (?)             |                     | `bsd/invFlags.yaml`                    |                                |                                   |
| Inventory groups                | `/groups`           | `bsd/groupIDs.yaml`                    | `universe/groups.yaml`         |                                   |
| Inventory items (?)             |                     | `bsd/invItems.yaml`                    |                                |                                   |
| Inventory names (?)             |                     | `bsd/invNames.yaml`                    |                                |                                   |
| Inventory positions (?)         |                     | `bsd/invPositions.yaml`                |                                |                                   |
| Inventory types                 | `/types`            | `fsd/typeIDs.yaml` - masteries, traits | `universe/types.yaml`          | Adds `repackagedvolumes.json`     |
| Inventory type traits           | `/types`            |                                        | `universe/types.yaml`          |                                   |
| Inventory type masteries        | `/types`            |                                        | `universe/types.yaml`          |                                   |
| Inventory unique names          |                     | `bsd/invUniqueNames.yaml`              |                                |                                   |
| Landmarks                       |                     | `fsd/landmarks/landmarks.staticdata`   |                                |                                   |
| Languages                       |                     | `fsd/translationLanguages.yaml`        | _Yes, indirectly_              | `localization_languages.json`     |
| Loyalty offers                  |                     |                                        | Yes                            |                                   |
| Market groups                   | `/market_groups`    | `fsd/marketGroups.yaml`                | `market/groups.yaml`           |                                   |
| Meta groups                     | `/meta_groups`      | `fsd/metaGroups.yaml`                  |                                |                                   |
| Moons                           |                     | `fsd/universe`                         | `universe/moons.yaml`          |                                   |
| NPC corporation divisions       |                     | `fsd/npcCorporationDivisions.yaml`     |                                |                                   |
| NPC corporation                 |                     | `fsd/npcCorporations.yaml`             |                                |                                   |
| Opportunity groups              |                     |                                        | `opportunities/groups.yaml`    |                                   |
| Opportunity tasks               |                     |                                        | `opportunities/tasks.yaml`     |                                   |
| Planetary schematics            |                     | `fsd/planetSchematics.yaml`            | `universe/schematics.yaml`     |                                   |
| Planets                         |                     | `fsd/universe`                         | `universe/planets.yaml`        |                                   |
| Races                           |                     | `fsd/races.yaml`                       | `universe/races.yaml`          |                                   |
| Regions                         |                     | `fsd/universe`                         | `universe/regions.yaml`        |                                   |
| Research agents                 |                     | `fsd/researchAgents.yaml`              |                                |                                   |
| Skills                          | `/skills `          | _types and dogma_                      | _types and dogma_              |                                   |
| Skin licenses                   |                     | `fsd/skinLicenses.yaml`                |                                |                                   |
| Skin materials                  |                     | `fsd/skinMaterials.yaml`               |                                | `skinmaterials.json`              |
| Skin material names             |                     |                                        |                                | `skinmaterialnames.json`          |
| Skins                           |                     | `fsd/skins.yaml`                       |                                | `skins.json`                      |
| Stargate                        |                     | `fsd/universe`                         | `universe/stargates.yaml`      |                                   |
| Stars                           |                     | `fsd/universe`                         | `universe/stars.yaml`          |                                   |
| Stations                        |                     | `bsd/staStations.yaml`                 | `universe/stations.yaml`       |                                   |
| Station operation               |                     | `fsd/stationOperations.yaml`           |                                |                                   |
| Station services                |                     | `fsd/stationServices.yaml`             |                                |                                   |
| Systems                         |                     | `fsd/universe`                         | `universe/systems.yaml`        |                                   |
| Tournament rule sets            |                     | `fsd/tournamentRuleSets.yaml`          |                                |                                   |
| Universes                       |                     | `fsd/universe`                         |                                |                                   |

* _The ESI filenames refer to the names in the ESI dump, minus the language suffix._

## Data structure

* Field names will be `snake_case`, since that's how the ESI does it and all other data on EVE Ref Data comes from there.\
  It makes sense to continue that format.
* The JSON layout will be structured in a way mostly inspired by the ESI, though that may not always be possible.
* Prefer keyed objects to arrays - the final object merger should be kept as simple as possible.\
  Since it's not possible to merge arrays in a predictable while preventing data duplication, keyed objects are preferred.
* Names and descriptions will use a language map like the SDE, rather than multiple files like the ESI.
* URLs will be `snake_case`, because that's how the ESI does it.

## Corrections (TBD)

It would be possible to maintain a series of corrections and additions to the data.
For instance, there are Dogma attributes which have no categories and these could be added.
There are also cases where Dogma values are stored "incorrectly". For instance, sometimes the number 10% is stored as `10.0` and other times as `0.1`,
even though the unit for the attribute is "percentage".
These could be corrected.

* Pro: The data is more accurate, consistent, and useful.
* Con: The data isn't a direct copy of the SDE or ESI.

