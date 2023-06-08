---
title: Reference Data
---
# Reference Data

The reference data build is currently in development.
<https://github.com/autonomouslogic/eve-ref/milestone/3>.
Any files [data.everef.net/ref-data](https://data.everef.net/ref-data) are not final and the format may change at any time.

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
* <https://ref-data.everef.net/dogma_attributes>
* <https://ref-data.everef.net/dogma_attributes/37>
* <https://ref-data.everef.net/skills>
* <https://ref-data.everef.net/skills/3336>

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

| Data                            | SDE                                     | ESI                             | Hoboleaks                |
|---------------------------------|-----------------------------------------|---------------------------------|--------------------------|
| Accounting Entry Types (?)      |                                         |                                 | Yes                      |
| Agent                           | `fsd/agents.yaml`                       |                                 |                          |
| Agent in space (?)              | `fsd/agentsInSpace.yaml`                |                                 |                          |
| Agent Types (?)                 |                                         |                                 | Yes                      |
| Ancestors                       | `fsd/ancestries.yaml`                   | Yes                             |                          |
| Asteroid belts                  | `fsd/universe`                          | Yes                             |                          |
| Bloodlines                      | `fsd/bloodlines.yaml`                   | Yes                             |                          |
| Blueprints                      | `fsd/blueprints.yaml`                   |                                 |                          |
| Blueprint materials             | `fsd/typeMaterials.yaml`                |                                 |                          |
| Certificates                    | `fsd/certificates.yaml`                 |                                 |                          |
| Character attributes            | `fsd/characterAttributes.yaml`          |                                 |                          |
| Clone States (?)                |                                         |                                 | Yes                      |
| Compressible Types (?)          |                                         |                                 | Yes                      |
| Constellations                  | `fsd/universe`                          | Yes                             |                          |
| Contraband types (?)            | `fsd/contrabandTypes.yaml`              |                                 |                          |
| Control tower resources (?)     | `fsd/controlTowerResources.yaml`        |                                 |                          |
| Corporatation activities (?)    | `fsd/corporationActivities.yaml`        |                                 |                          |
| Dbuffs (?)                      |                                         |                                 | Yes                      |
| Dogma attributes categories     | `fsd/dogmaAttributeCategories.yaml`     |                                 |                          |
| Dogma attributes                | `fsd/dogmaAttributes.yaml`              | `dogma/attributes.yaml`         | Localised names          |
| Dogma effects                   | `fsd/dogmaEffects.yaml`                 | `dogma/effects.yaml`            |                          |
| Dogma expressions               |                                         |                                 |                          |
| Dogma type attributes           | `fsd/typeDogma.yaml`                    | `universe/types.yaml`           |                          |
| Dogma type effects              | `fsd/typeDogma.yaml`                    | `universe/types.yaml`           |                          |
| Dogma units                     |                                         |                                 | `dogmaunits.json`        |
| Dynamic Attributes (?)          |                                         |                                 | Yes                      |
| Factions                        | `fsd/factions.yaml`                     | Yes                             |                          |
| Graphics                        | `fsd/graphicIDs.yaml`                   | Yes                             |                          |
| Graphic Material Sets (?)       |                                         |                                 | Yes                      |
| Icons                           | `fsd/iconIDs.yaml`                      |                                 |                          |
| Industry Activities (?)         |                                         |                                 | Yes                      |
| Industry Assembly Lines (?)     |                                         |                                 | Yes                      |
| Industry Installation Types (?) |                                         |                                 | Yes                      |
| Industry Modifier Sources (?)   |                                         |                                 | Yes                      |
| Industry Target Filters (?)     |                                         |                                 | Yes                      |
| Inventory categories            | `fsd/categoryIDs.yaml`                  | Yes                             |                          |
| Inventory flags (?)             | `bsd/invFlags.yaml`                     |                                 |                          |
| Inventory groups                | `bsd/groupIDs.yaml`                     | Yes                             |                          |
| Inventory items (?)             | `bsd/invItems.yaml`                     |                                 |                          |
| Inventory names (?)             | `bsd/invNames.yaml`                     |                                 |                          |
| Inventory positions (?)         | `bsd/invPositions.yaml`                 |                                 |                          |
| Inventory types                 | `fsd/typeIDs.yaml` - masteries, traits  | `universe/types.*.yaml` - dogma | `repackagedvolumes.json` |
| Inventory unique names          | `bsd/invUniqueNames.yaml`               |                                 |                          |
| Landmarks                       | `fsd/landmarks/landmarks.staticdata`    |                                 |                          |
| Languages                       | `fsd/translationLanguages.yaml`         | Yes, indirectly                 | Yes                      |
| Loyalty offers                  |                                         | Yes                             |                          |
| Market groups                   | `fsd/marketGroups.yaml`                 | Yes                             |                          |
| Meta groups                     | `fsd/metaGroups.yaml`                   |                                 |                          |
| Moons                           | `fsd/universe`                          | Yes                             |                          |
| NPC corporation divisions       | `fsd/npcCorporationDivisions.yaml`      |                                 |                          |
| NPC corporation                 | `fsd/npcCorporations.yaml`              |                                 |                          |
| Opportunity groups              |                                         | Yes                             |                          |
| Opportunity tasks               |                                         | Yes                             |                          |
| Planetary schematics            | `fsd/planetSchematics.yaml`             | Yes - no index                  |                          |
| Planets                         | `fsd/universe`                          | Yes                             |                          |
| Races                           | `fsd/races.yaml`                        | Yes                             |                          |
| Regions                         | `fsd/universe`                          | Yes                             |                          |
| Research agents                 | `fsd/researchAgents.yaml`               |                                 |                          |
| Skin licenses                   | `fsd/skinLicenses.yaml`                 |                                 |                          |
| Skin materials                  | `fsd/skinMaterials.yaml`                |                                 | Yes                      |
| Skin material names             |                                         |                                 | Yes                      |
| Skins                           | `fsd/skins.yaml`                        |                                 | Yes                      |
| Stargate                        | `fsd/universe`                          | Yes                             |                          |
| Stars                           | `fsd/universe`                          | Yes                             |                          |
| Stations                        | `bsd/staStations.yaml`                  | Yes                             |                          |
| Station operation               | `fsd/stationOperations.yaml`            |                                 |                          |
| Station services                | `fsd/stationServices.yaml`              |                                 |                          |
| Systems                         | `fsd/universe`                          | Yes                             |                          |
| Tournament rule sets            | `fsd/tournamentRuleSets.yaml`           |                                 |                          |
| Universes                       | `fsd/universe`                          |                                 |                          |

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

