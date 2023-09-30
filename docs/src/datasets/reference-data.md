---
title: Reference Data
---
# Reference Data

The Reference Data set is a collection of data from the EVE Online SDE, ESI, and Hoboleaks data.
It aims to be a single source, combining all the available data into one.
It does this by taking the latest [SDE](https://developers.eveonline.com/resource/resources),
[ESI scrape](https://data.everef.net/esi-scrape/), and [Hoboleaks export](https://sde.hoboleaks.space/) and merging them
into one common format.

The Reference Data is available as a REST API with full OpenAPI spec and as a [full download](https://data.everef.net/reference-data/).

## Development
The reference data is currently in development.
While changes should be minimal, they may occur at any time. 

## REST API
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
* <https://ref-data.everef.net/dogma_effects>
* <https://ref-data.everef.net/dogma_effects/11>
* <https://ref-data.everef.net/skills>
* <https://ref-data.everef.net/skills/3336>
* <https://ref-data.everef.net/mutaplasmids>
* <https://ref-data.everef.net/mutaplasmids/52225>
* <https://ref-data.everef.net/units>
* <https://ref-data.everef.net/units/1>
* <https://ref-data.everef.net/blueprints>
* <https://ref-data.everef.net/blueprints/999>
* <https://ref-data.everef.net/regions>
* <https://ref-data.everef.net/regions/12000001>

## Motivation
Two primary datasets are available for third-party developers of EVE Online: the SDE and ESI.
While comprehensive, these two are not equal.
There is data in the SDE which isn't in the ESI, and vice-versa.
Additionally, Hoboleaks provides data extracted from the EVE Online client files.

EVE Ref was originally built on taking all three sources and combining them into one,
making it as comprehensive as possible.
The Reference Data set is an attempt at publishing this data for other developers to consume.

## Data sources
This table show the available data and where to get it. 

| Data                          | Reference data      | SDE                                    | ESI                             | Hoboleaks                           |
|-------------------------------|---------------------|----------------------------------------|---------------------------------|-------------------------------------|
| Accounting entry types        |                     |                                        |                                 | `accountingentrytypes.json`         |
| Agent                         |                     | `fsd/agents.yaml`                      |                                 |                                     |
| Agent in space                |                     | `fsd/agentsInSpace.yaml`               |                                 |                                     |
| Agent types                   |                     |                                        |                                 | `agenttypes.json`                   |
| Ancestors                     |                     | `fsd/ancestries.yaml`                  | `universe/ancestries.yaml`      |                                     |
| Asteroid belts                |                     | `fsd/universe`                         | `universe/asteroid_belts.yaml`  |                                     |
| Bloodlines                    |                     | `fsd/bloodlines.yaml`                  | `universe/bloodlines.yaml`      |                                     |
| Blueprints                    | `/blueprints`       | `fsd/blueprints.yaml`                  |                                 | `blueprints.json`                   |
| Certificates                  |                     | `fsd/certificates.yaml`                |                                 |                                     |
| Character attributes          |                     | `fsd/characterAttributes.yaml`         |                                 |                                     |
| Clone states                  |                     |                                        |                                 | `clonestates.json`                  |
| Compressible types            |                     |                                        |                                 | `compressibletypes.json`            |
| Constellations                |                     | `fsd/universe`                         | `universe/constellations.yaml`  |                                     |
| Contraband types              |                     | `fsd/contrabandTypes.yaml`             |                                 |                                     |
| Control tower resources       |                     | `fsd/controlTowerResources.yaml`       |                                 |                                     |
| Corporation activities        |                     | `fsd/corporationActivities.yaml`       |                                 |                                     |
| Dbuffs                        |                     |                                        |                                 | `dbuffs.json`                       |
| Dogma attributes categories   |                     | `fsd/dogmaAttributeCategories.yaml`    |                                 |                                     |
| Dogma attributes              | `/dogma_attributes` | `fsd/dogmaAttributes.yaml`             | `dogma/attributes.yaml`         | `localization_dgmattributes.json`   |
| Dogma effects                 | `/dogma_effects`    | `fsd/dogmaEffects.yaml`                | `dogma/effects.yaml`            |                                     |
| Dogma expressions             |                     |                                        |                                 |                                     |
| Dogma type attributes         | `/types`            | `fsd/typeDogma.yaml`                   | `universe/types.yaml`           |                                     |
| Dogma type effects            |                     | `fsd/typeDogma.yaml`                   | `universe/types.yaml`           |                                     |
| Dogma units                   |                     |                                        |                                 | `dogmaunits.json`                   |
| Dynamic attributes            | `/mutaplasmids`     |                                        |                                 | `dynamicitemattributes.json`        |
| Expert systems                |                     |                                        |                                 | `expertsystems.json`                |
| Factions                      |                     | `fsd/factions.yaml`                    | `universe/factions.yaml`        |                                     |
| Graphics                      |                     | `fsd/graphicIDs.yaml`                  | `universe/graphics.yaml`        |                                     |
| Graphic material sets         |                     |                                        |                                 | `graphicmaterialsets.json`          |
| Icons                         |                     | `fsd/iconIDs.yaml`                     |                                 |                                     |
| Industry activities           |                     |                                        |                                 | `industryactivities.json`           |
| Industry assembly lines       |                     |                                        |                                 | `industryassemblylines.json`        |
| Industry installation types   |                     |                                        |                                 | `industryinstallationtypes.json`    |
| Industry modifier sources     |                     |                                        |                                 | `industrymodifiersources.json`      |
| Industry target filters       |                     |                                        |                                 | `industrytargetfilters.json`        |
| Inventory categories          | `/categories`       | `fsd/categoryIDs.yaml`                 | `universe/categories.yaml`      |                                     |
| Inventory flags               |                     | `bsd/invFlags.yaml`                    |                                 |                                     |
| Inventory groups              | `/groups`           | `bsd/groupIDs.yaml`                    | `universe/groups.yaml`          |                                     |
| Inventory items               |                     | `bsd/invItems.yaml`                    |                                 |                                     |
| Inventory names               |                     | `bsd/invNames.yaml`                    |                                 |                                     |
| Inventory positions           |                     | `bsd/invPositions.yaml`                |                                 |                                     |
| Inventory types               | `/types`            | `fsd/typeIDs.yaml` - masteries, traits | `universe/types.yaml`           | Adds `repackagedvolumes.json`       |
| Inventory type traits         | `/types`            |                                        | `universe/types.yaml`           |                                     |
| Inventory type masteries      | `/types`            |                                        | `universe/types.yaml`           |                                     |
| Inventory unique names        |                     | `bsd/invUniqueNames.yaml`              |                                 |                                     |
| Landmarks                     |                     | `fsd/landmarks/landmarks.staticdata`   |                                 |                                     |
| Languages                     |                     | `fsd/translationLanguages.yaml`        | _Yes, indirectly_               | `localization_languages.json`       |
| Loyalty offers                |                     |                                        | Yes                             |                                     |
| Market groups                 | `/market_groups`    | `fsd/marketGroups.yaml`                | `market/groups.yaml`            |                                     |
| Meta groups                   | `/meta_groups`      | `fsd/metaGroups.yaml`                  |                                 |                                     |
| Moons                         |                     | `fsd/universe`                         | `universe/moons.yaml`           |                                     |
| NPC corporation divisions     |                     | `fsd/npcCorporationDivisions.yaml`     |                                 |                                     |
| NPC corporation               |                     | `fsd/npcCorporations.yaml`             |                                 |                                     |
| Opportunity groups            |                     |                                        | `opportunities/groups.yaml`     |                                     |
| Opportunity tasks             |                     |                                        | `opportunities/tasks.yaml`      |                                     |
| Planetary schematics          |                     | `fsd/planetSchematics.yaml`            | `universe/schematics.yaml`      |                                     |
| Planets                       |                     | `fsd/universe`                         | `universe/planets.yaml`         |                                     |
| Races                         |                     | `fsd/races.yaml`                       | `universe/races.yaml`           |                                     |
| Regions                       | `/regions`          | `fsd/universe`                         | `universe/regions.yaml`         |                                     |
| Reprocessing                  | `/types`            | `fsd/typeMaterials.yaml`               |                                 | `typematerials.json`                |
| Research agents               |                     | `fsd/researchAgents.yaml`              |                                 |                                     |
| Schools                       |                     |                                        |                                 | `schools.json`                      |
| School map                    |                     |                                        |                                 | `schoolmap.json`                    |
| Skills                        | `/skills `          | _types and dogma_                      | _types and dogma_               |                                     |
| Skill plans                   |                     |                                        |                                 | `skillplans.json`                   |
| Skin licenses                 |                     | `fsd/skinLicenses.yaml`                |                                 |                                     |
| Skin materials                |                     | `fsd/skinMaterials.yaml`               |                                 | `skinmaterials.json`                |
| Skin material names           |                     |                                        |                                 | `skinmaterialnames.json`            |
| Skins                         |                     | `fsd/skins.yaml`                       |                                 | `skins.json`                        |
| Stargate                      |                     | `fsd/universe`                         | `universe/stargates.yaml`       |                                     |
| Stars                         |                     | `fsd/universe`                         | `universe/stars.yaml`           |                                     |
| Stations                      |                     | `bsd/staStations.yaml`                 | `universe/stations.yaml`        |                                     |
| Station operation             |                     | `fsd/stationOperations.yaml`           |                                 |                                     |
| Station services              |                     | `fsd/stationServices.yaml`             |                                 |                                     |
| Station standing restrictions |                     |                                        |                                 | `stationstandingsrestrictions.json` |
| Systems                       |                     | `fsd/universe`                         | `universe/systems.yaml`         |                                     |
| Tournament rule sets          |                     | `fsd/tournamentRuleSets.yaml`          |                                 |                                     |
| Universes                     |                     | `fsd/universe`                         |                                 |                                     |

* _The ESI filenames refer to the names in the ESI scrape, minus the language suffix._

## Data structure

* Field names will be `snake_case`, since that's how the ESI does it and all other data on EVE Ref Data comes from there.\
  It makes sense to continue that format.
* URLs will be `snake_case`, because that's how the ESI does it.
* The JSON layout will be structured in a way mostly inspired by the ESI, though that may not always be possible.
* Prefer keyed objects to arrays - the final object merger should be kept as simple as possible.\
  Since it's not possible to merge arrays in a predictable way while preventing data duplication, keyed objects are preferred.
* Names and descriptions will use a language map like the SDE, rather than multiple endpoints/files like the ESI.

## Corrections (TBD)

It would be possible to maintain a series of corrections and additions to the data.
For instance, there are Dogma attributes which have no categories and these could be added.
There are also cases where Dogma values are stored "incorrectly". For instance, sometimes the number 10% is stored as `10.0` and other times as `0.1`,
even though the unit for the attribute is "percentage".
These could be corrected.

* Pro: The data is more accurate, consistent, and useful.
* Con: The data isn't a direct copy of the SDE or ESI.

