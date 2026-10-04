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
* <https://ref-data.everef.net/agent_types>
* <https://ref-data.everef.net/agent_types/2>
* <https://ref-data.everef.net/asteroid_belts>
* <https://ref-data.everef.net/asteroid_belts/40000003>
* <https://ref-data.everef.net/blueprints>
* <https://ref-data.everef.net/blueprints/999>
* <https://ref-data.everef.net/categories>
* <https://ref-data.everef.net/categories/4>
* <https://ref-data.everef.net/constellations>
* <https://ref-data.everef.net/constellations/20000001>
* <https://ref-data.everef.net/dogma_attributes>
* <https://ref-data.everef.net/dogma_attributes/37>
* <https://ref-data.everef.net/dogma_effects>
* <https://ref-data.everef.net/dogma_effects/11>
* <https://ref-data.everef.net/groups>
* <https://ref-data.everef.net/groups/18>
* <https://ref-data.everef.net/icons>
* <https://ref-data.everef.net/icons/67>
* <https://ref-data.everef.net/market_groups>
* <https://ref-data.everef.net/market_groups/1857>
* <https://ref-data.everef.net/meta_groups>
* <https://ref-data.everef.net/meta_groups/6>
* <https://ref-data.everef.net/moons>
* <https://ref-data.everef.net/moons/40000004>
* <https://ref-data.everef.net/mutaplasmids>
* <https://ref-data.everef.net/mutaplasmids/52225>
* <https://ref-data.everef.net/npc_characters>
* <https://ref-data.everef.net/npc_characters/3008416>
* <https://ref-data.everef.net/npc_corporations>
* <https://ref-data.everef.net/npc_corporations/1000002>
* <https://ref-data.everef.net/npc_corporation_divisions>
* <https://ref-data.everef.net/npc_corporation_divisions/18>
* <https://ref-data.everef.net/npc_stations>
* <https://ref-data.everef.net/npc_stations/60012526>
* <https://ref-data.everef.net/planets>
* <https://ref-data.everef.net/planets/40000002>
* <https://ref-data.everef.net/regions>
* <https://ref-data.everef.net/regions/10000002>
* <https://ref-data.everef.net/schematics>
* <https://ref-data.everef.net/schematics/65>
* <https://ref-data.everef.net/skills>
* <https://ref-data.everef.net/skills/3336>
* <https://ref-data.everef.net/solar_systems>
* <https://ref-data.everef.net/solar_systems/30000001>
* <https://ref-data.everef.net/stargates>
* <https://ref-data.everef.net/stargates/50000056>
* <https://ref-data.everef.net/stars>
* <https://ref-data.everef.net/stars/40000001>
* <https://ref-data.everef.net/station_operations>
* <https://ref-data.everef.net/station_operations/33>
* <https://ref-data.everef.net/station_services>
* <https://ref-data.everef.net/station_services/1>
* <https://ref-data.everef.net/types>
* <https://ref-data.everef.net/types/645>
* <https://ref-data.everef.net/units>
* <https://ref-data.everef.net/units/1>

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

| Data                          | Reference data               | Static Data                                 | ESI                            | Hoboleaks                           |
|-------------------------------|------------------------------|---------------------------------------------|--------------------------------|-------------------------------------|
| Accounting entry types        |                              | `accountingEntryTypes.jsonl`                |                                | `accountingentrytypes.json`         |
| Agent in space                |                              | `agentsInSpace.jsonl`                       |                                |                                     |
| Agent types                   | `/agent_types`               | `agentTypes.jsonl`                          |                                | `agenttypes.json`                   |
| Ancestors                     |                              | `ancestries.jsonl`                          | `universe/ancestries.yaml`     |                                     |
| Applied proximity effects     |                              | `appliedProximityEffects.jsonl`             |                                |                                     |
| Archetypes                    |                              | `archetypes.jsonl`                          |                                |                                     |
| Asteroid belts                | `/asteroid_belts`            | `mapAsteroidBelts.jsonl`                    | `universe/asteroid_belts.yaml` |                                     |
| Bloodlines                    |                              | `bloodlines.jsonl`                          | `universe/bloodlines.yaml`     |                                     |
| Blueprints                    | `/blueprints`                | `blueprints.jsonl`                          |                                | `blueprints.json`                   |
| Certificates                  |                              | `certificates.jsonl`                        |                                |                                     |
| Character attributes          |                              | `characterAttributes.jsonl`                 |                                |                                     |
| Character titles              |                              | `characterTitles.jsonl`                     |                                |                                     |
| Clone grades                  |                              | `cloneGrades.jsonl`                         |                                | `clonestates.json`                  |
| Compressible types            |                              | `compressibleTypes.jsonl`                   |                                | `compressibletypes.json`            |
| Constellations                | `/constellations`            | `mapConstellations.jsonl`                   | `universe/constellations.yaml` |                                     |
| Contraband types              |                              | `contrabandTypes.jsonl`                     |                                |                                     |
| Control tower resources       |                              | `controlTowerResources.jsonl`               |                                |                                     |
| Corporation activities        |                              | `corporationActivities.jsonl`               |                                |                                     |
| Corporation role groups       |                              | `corporationRoleGroups.jsonl`               |                                |                                     |
| Corporation roles             |                              | `corporationRoles.jsonl`                    |                                |                                     |
| Dbuffs                        |                              | `dbuffCollections.jsonl`                    |                                | `dbuffs.json`                       |
| Dogma attributes              | `/dogma_attributes`          | `dogmaAttributes.jsonl`                     | `dogma/attributes.yaml`        | `localization_dgmattributes.json`   |
| Dogma attributes categories   |                              | `dogmaAttributeCategories.jsonl`            |                                |                                     |
| Dogma effects                 | `/dogma_effects`             | `dogmaEffects.jsonl`                        | `dogma/effects.yaml`           |                                     |
| Dogma expressions             |                              |                                             |                                |                                     |
| Dogma type attributes         | `/types`                     | `typeDogma.jsonl`                           | `universe/types.yaml`          |                                     |
| Dogma type effects            |                              |                                             | `universe/types.yaml`          |                                     |
|                               |                              |                                             |                                | `dogmaeffectcategories.json`        |
|                               |                              |                                             |                                | `attributeorders.json`              |
| Dogma units                   |                              | `dogmaUnits.jsonl`                          |                                | `dogmaunits.json`                   |
| Dungeons                      |                              | `dungeons.jsonl`                            |                                |                                     |
| Dynamic attributes            | `/mutaplasmids`              | `dynamicItemAttributes.jsonl`               |                                | `dynamicitemattributes.json`        |
| Epic arcs                     |                              | `epicArcs.jsonl`                            |                                |                                     |
| Expert systems                |                              | `expertSystems.jsonl`                       |                                | `expertsystems.json`                |
| Factions                      |                              | `factions.jsonl`                            | `universe/factions.yaml`       |                                     |
| Fighter abilities             |                              | `fighterAbilities.jsonl`                    |                                |                                     |
| Fighter abilities by type     |                              | `fighterAbilitiesByType.jsonl`              |                                |                                     |
| Freelance job schemas         |                              | `freelanceJobSchemas.jsonl`                 |                                |                                     |
| Graphic material sets         |                              | `graphicMaterialSets.jsonl`                 |                                | `graphicmaterialsets.json`          |
| Graphics                      |                              | `graphics.jsonl`                            | `universe/graphics.yaml`       |                                     |
| Icons                         | `/icons`                     | `icons.jsonl`                               |                                |                                     |
| Industry activities           |                              | `industryActivities.jsonl`                  |                                | `industryactivities.json`           |
| Industry assembly lines       |                              | `industryAssemblyLines.jsonl`               |                                | `industryassemblylines.json`        |
| Industry installation types   |                              | `industryInstallationTypes.jsonl`           |                                | `industryinstallationtypes.json`    |
| Industry modifier sources     |                              | `industryModifierSources.jsonl`             |                                | `industrymodifiersources.json`      |
| Industry target filters       |                              | `industryTargetFilters.jsonl`               |                                | `industrytargetfilters.json`        |
| Inventory categories          | `/categories`                | `categories.jsonl`                          | `universe/categories.yaml`     |                                     |
| Inventory flags               |                              | `bsd/invFlags.jsonl` in old files           |                                |                                     |
| Inventory groups              | `/groups`                    | `groups.jsonl`                              | `universe/groups.yaml`         |                                     |
| Inventory items               |                              | `bsd/invItems.jsonl` in old files           |                                |                                     |
| Inventory names               |                              | `bsd/invNames.jsonl` in old files           |                                |                                     |
| Inventory type masteries      | `/types`                     | `masteries.jsonl`                           | `universe/types.yaml`          |                                     |
| Inventory type traits         | `/types`                     | `typeBonus.jsonl`                           | `universe/types.yaml`          |                                     |
| Inventory types               | `/types`                     | `types.jsonl`                               | `universe/types.yaml`          | Adds `repackagedvolumes.json`       |
| Inventory unique names        |                              | `bsd/invUniqueNames.jsonl` in old files     |                                |                                     |
| Landmarks                     |                              | `landmarks.jsonl`                           |                                |                                     |
| Languages                     |                              | `translationLanguages.jsonl`                | _Yes, indirectly_              | `localization_languages.json`       |
| Link with ship                |                              | `linkWithShip.jsonl`                        |                                |                                     |
| Loyalty offers                |                              |                                             | Yes                            |                                     |
| Market groups                 | `/market_groups`             | `marketGroups.jsonl`                        | `market/groups.yaml`           |                                     |
| Mercenary tactical operations |                              | `mercenaryTacticalOperations.jsonl`         |                                |                                     |
| Meta groups                   | `/meta_groups`               | `metaGroups.jsonl`                          |                                |                                     |
| Metenox moon drill            |                              | `metenoxMoonDrill.jsonl`                    |                                |                                     |
| Military campaign objectives  |                              | `militaryCampaignObjectives.jsonl`          |                                |                                     |
| Military campaigns            |                              | `militaryCampaigns.jsonl`                   |                                |                                     |
| Missions                      |                              | `missions.jsonl`                            |                                |                                     |
| Moons                         | `/moons`                     | `mapMoons.jsonl`                            | `universe/moons.yaml`          |                                     |
| Notification types            |                              | `notificationTypes.jsonl`                   |                                |                                     |
| NPC characters                | `/npc_characters`            | `npcCharacters.jsonl`                       |                                |                                     |
| NPC corporation               | `/npc_corporations`          | `npcCorporations.jsonl`                     |                                |                                     |
| NPC corporation divisions     | `/npc_corporation_divisions` | `npcCorporationDivisions.jsonl`             |                                |                                     |
| Opportunity groups            |                              |                                             | `opportunities/groups.yaml`    |                                     |
| Opportunity tasks             |                              |                                             | `opportunities/tasks.yaml`     |                                     |
| Planet resources              |                              | `planetResources.jsonl`                     |                                |                                     |
| Planetary schematics          | `/schematics`                | `planetSchematics.jsonl`                    | `universe/schematics.yaml`     |                                     |
| Planets                       | `/planets`                   | `mapPlanets.jsonl`                          | `universe/planets.yaml`        |                                     |
| Proximity traps               |                              | `proximityTrap.jsonl`                       |                                |                                     |
| Races                         |                              | `races.jsonl`                               | `universe/races.yaml`          |                                     |
| Regions                       | `/regions`                   | `mapRegions.jsonl`                          | `universe/regions.yaml`        |                                     |
| Reprocessing                  | `/types`                     | `typeMaterials.jsonl`                       |                                | `typematerials.json`                |
| School map                    |                              | `schoolMap.jsonl`                           |                                | `schoolmap.json`                    |
| Schools                       |                              | `schools.jsonl`                             |                                | `schools.json`                      |
| Secondary suns                |                              | `mapSecondarySuns.jsonl`                    |                                |                                     |
| Ship tree elements            |                              | `shipTreeElements.jsonl`                    |                                |                                     |
| Ship tree factions            |                              | `shipTreeFactions.jsonl`                    |                                |                                     |
| Ship tree groups              |                              | `shipTreeGroups.jsonl`                      |                                |                                     |
| Skill plans                   |                              | `skillPlans.jsonl`                          |                                | `skillplans.json`                   |
| Skills                        | `/skills `                   | _types and dogma_                           | _types and dogma_              |                                     |
| Skin licenses                 |                              | `skinLicenses.jsonl`                        |                                |                                     |
| Skin material names           |                              |                                             |                                | `skinmaterialnames.json`            |
| Skin materials                |                              | `skinMaterials.jsonl`                       |                                | `skinmaterials.json`                |
| Skinr component categories    |                              | `skinrComponentCategories.jsonl`            |                                |                                     |
| Skinr component point values  |                              | `skinrComponentPointValues.jsonl`           |                                |                                     |
| Skinr component rarities      |                              | `skinrComponentRarities.jsonl`              |                                |                                     |
| Skinr components              |                              | `skinrComponents.jsonl`                     |                                |                                     |
| Skinr slot categories         |                              | `skinrSlotCategories.jsonl`                 |                                |                                     |
| Skinr slot configurations     |                              | `skinrSlotConfigurations.jsonl`             |                                |                                     |
| Skinr slot names              |                              | `skinrSlotNames.jsonl`                      |                                |                                     |
| Skinr slots                   |                              | `skinrSlots.jsonl`                          |                                |                                     |
| Skinr slots to materials      |                              | `skinrSlotsToMaterials.jsonl`               |                                |                                     |
| Skinr tier thresholds         |                              | `skinrTierThresholds.jsonl`                 |                                |                                     |
| Skins                         |                              | `skins.jsonl`                               |                                | `skins.json`                        |
| Sovereignty upgrades          |                              | `sovereigntyUpgrades.jsonl`                 |                                |                                     |
| Stargate                      | `/stargates`                 | `mapStargates.jsonl`                        | `universe/stargates.yaml`      |                                     |
| Stars                         | `/stars`                     | `mapStars.jsonl`                            | `universe/stars.yaml`          |                                     |
| Station operation             | `/station_operations`        | `stationOperations.jsonl`                   |                                |                                     |
| Station services              | `/station_services`          | `stationServices.jsonl`                     |                                |                                     |
| Station standing restrictions |                              | `stationStandingsRestrictions.jsonl`        |                                | `stationstandingsrestrictions.json` |
| Stations                      | `/npc_stations`              | `npcStations.jsonl`                         | `universe/stations.yaml`       |                                     |
| System dbuff emitters         |                              | `systemDbuffEmitters.jsonl`                 |                                |                                     |
| System wide effects           |                              | `systemWideEffects.jsonl`                   |                                |                                     |
| Systems                       | `/solar_systems`             | `mapSolarSystems.jsonl`                     | `universe/systems.yaml`        |                                     |
| Tournament rule sets          |                              | `fsd/tournamentRuleSets.jsonl` in old files |                                |                                     |
| Type elements                 |                              | `typeElements.jsonl`                        |                                |                                     |
| Type lists                    |                              | `typeLists.jsonl`                           |                                |                                     |

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
