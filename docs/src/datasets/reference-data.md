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
* <https://ref-data.everef.net/blueprints>
* <https://ref-data.everef.net/blueprints/999>
* <https://ref-data.everef.net/categories>
* <https://ref-data.everef.net/categories/4>
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
* <https://ref-data.everef.net/mutaplasmids>
* <https://ref-data.everef.net/mutaplasmids/52225>
* <https://ref-data.everef.net/regions>
* <https://ref-data.everef.net/regions/10000002>
* <https://ref-data.everef.net/schematics>
* <https://ref-data.everef.net/schematics/65>
* <https://ref-data.everef.net/skills>
* <https://ref-data.everef.net/skills/3336>
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

| Data                          | Reference data      | SDE                                        | ESI                            | Hoboleaks                           |
|-------------------------------|---------------------|--------------------------------------------|--------------------------------|-------------------------------------|
| Accounting entry types        |                     | `accountingEntryTypes.yaml`                |                                | `accountingentrytypes.json`         |
| Agent                         |                     | `npcCharacters.yaml`                       |                                |                                     |
| Agent in space                |                     | `agentsInSpace.yaml`                       |                                |                                     |
| Agent types                   |                     | `agentTypes.yaml`                          |                                | `agenttypes.json`                   |
| Ancestors                     |                     | `ancestries.yaml`                          | `universe/ancestries.yaml`     |                                     |
| Applied proximity effects     |                     | `appliedProximityEffects.yaml`             |                                |                                     |
| Archetypes                    |                     | `archetypes.yaml`                          |                                |                                     |
| Asteroid belts                |                     | `mapAsteroidBelts.yaml`                    | `universe/asteroid_belts.yaml` |                                     |
| Bloodlines                    |                     | `bloodlines.yaml`                          | `universe/bloodlines.yaml`     |                                     |
| Blueprints                    | `/blueprints`       | `blueprints.yaml`                          |                                | `blueprints.json`                   |
| Certificates                  |                     | `certificates.yaml`                        |                                |                                     |
| Character attributes          |                     | `characterAttributes.yaml`                 |                                |                                     |
| Character titles              |                     | `characterTitles.yaml`                     |                                |                                     |
| Clone grades                  |                     | `cloneGrades.yaml`                         |                                | `clonestates.json`                  |
| Compressible types            |                     | `compressibleTypes.yaml`                   |                                | `compressibletypes.json`            |
| Constellations                |                     | `mapConstellations.yaml`                   | `universe/constellations.yaml` |                                     |
| Contraband types              |                     | `contrabandTypes.yaml`                     |                                |                                     |
| Control tower resources       |                     | `controlTowerResources.yaml`               |                                |                                     |
| Corporation activities        |                     | `corporationActivities.yaml`               |                                |                                     |
| Corporation role groups       |                     | `corporationRoleGroups.yaml`               |                                |                                     |
| Corporation roles             |                     | `corporationRoles.yaml`                    |                                |                                     |
| Dbuffs                        |                     | `dbuffCollections.yaml`                    |                                | `dbuffs.json`                       |
| Dogma attributes              | `/dogma_attributes` | `dogmaAttributes.yaml`                     | `dogma/attributes.yaml`        | `localization_dgmattributes.json`   |
| Dogma attributes categories   |                     | `dogmaAttributeCategories.yaml`            |                                |                                     |
| Dogma effects                 | `/dogma_effects`    | `dogmaEffects.yaml`                        | `dogma/effects.yaml`           |                                     |
| Dogma expressions             |                     |                                            |                                |                                     |
| Dogma type attributes         | `/types`            | `typeDogma.yaml`                           | `universe/types.yaml`          |                                     |
| Dogma type effects            |                     | `dogmaEffects.yaml`                        | `universe/types.yaml`          |                                     |
|                               |                     |                                            |                                | `dogmaeffectcategories.json`        |
|                               |                     |                                            |                                | `attributeorders.json`              |
| Dogma units                   |                     | `dogmaUnits.yaml`                          |                                | `dogmaunits.json`                   |
| Dungeons                      |                     | `dungeons.yaml`                            |                                |                                     |
| Dynamic attributes            | `/mutaplasmids`     | `dynamicItemAttributes.yaml`               |                                | `dynamicitemattributes.json`        |
| Epic arcs                     |                     | `epicArcs.yaml`                            |                                |                                     |
| Expert systems                |                     | `expertSystems.yaml`                       |                                | `expertsystems.json`                |
| Factions                      |                     | `factions.yaml`                            | `universe/factions.yaml`       |                                     |
| Fighter abilities             |                     | `fighterAbilities.yaml`                    |                                |                                     |
| Fighter abilities by type     |                     | `fighterAbilitiesByType.yaml`              |                                |                                     |
| Freelance job schemas         |                     | `freelanceJobSchemas.yaml`                 |                                |                                     |
| Graphic material sets         |                     | `graphicMaterialSets.yaml`                 |                                | `graphicmaterialsets.json`          |
| Graphics                      |                     | `graphics.yaml`                            | `universe/graphics.yaml`       |                                     |
| Icons                         | `/icons`            | `icons.yaml`                               |                                |                                     |
| Industry activities           |                     | `industryActivities.yaml`                  |                                | `industryactivities.json`           |
| Industry assembly lines       |                     | `industryAssemblyLines.yaml`               |                                | `industryassemblylines.json`        |
| Industry installation types   |                     | `industryInstallationTypes.yaml`           |                                | `industryinstallationtypes.json`    |
| Industry modifier sources     |                     | `industryModifierSources.yaml`             |                                | `industrymodifiersources.json`      |
| Industry target filters       |                     | `industryTargetFilters.yaml`               |                                | `industrytargetfilters.json`        |
| Inventory categories          | `/categories`       | `categories.yaml`                          | `universe/categories.yaml`     |                                     |
| Inventory flags               |                     | `bsd/invFlags.yaml` in old files           |                                |                                     |
| Inventory groups              | `/groups`           | `groups.yaml`                              | `universe/groups.yaml`         |                                     |
| Inventory items               |                     | `bsd/invItems.yaml` in old files           |                                |                                     |
| Inventory names               |                     | `bsd/invNames.yaml` in old files           |                                |                                     |
| Inventory type masteries      | `/types`            | `masteries.yaml`                           | `universe/types.yaml`          |                                     |
| Inventory type traits         | `/types`            | `typeBonus.yaml`                           | `universe/types.yaml`          |                                     |
| Inventory types               | `/types`            | `types.yaml`                               | `universe/types.yaml`          | Adds `repackagedvolumes.json`       |
| Inventory unique names        |                     | `bsd/invUniqueNames.yaml` in old files     |                                |                                     |
| Landmarks                     |                     | `landmarks.yaml`                           |                                |                                     |
| Languages                     |                     | `translationLanguages.yaml`                | _Yes, indirectly_              | `localization_languages.json`       |
| Link with ship                |                     | `linkWithShip.yaml`                        |                                |                                     |
| Loyalty offers                |                     |                                            | Yes                            |                                     |
| Market groups                 | `/market_groups`    | `marketGroups.yaml`                        | `market/groups.yaml`           |                                     |
| Mercenary tactical operations |                     | `mercenaryTacticalOperations.yaml`         |                                |                                     |
| Meta groups                   | `/meta_groups`      | `metaGroups.yaml`                          |                                |                                     |
| Metenox moon drill            |                     | `metenoxMoonDrill.yaml`                    |                                |                                     |
| Military campaign objectives  |                     | `militaryCampaignObjectives.yaml`          |                                |                                     |
| Military campaigns            |                     | `militaryCampaigns.yaml`                   |                                |                                     |
| Missions                      |                     | `missions.yaml`                            |                                |                                     |
| Moons                         |                     | `mapMoons.yaml`                            | `universe/moons.yaml`          |                                     |
| Notification types            |                     | `notificationTypes.yaml`                   |                                |                                     |
| NPC corporation               |                     | `npcCorporations.yaml`                     |                                |                                     |
| NPC corporation divisions     |                     | `npcCorporationDivisions.yaml`             |                                |                                     |
| Opportunity groups            |                     |                                            | `opportunities/groups.yaml`    |                                     |
| Opportunity tasks             |                     |                                            | `opportunities/tasks.yaml`     |                                     |
| Planet resources              |                     | `planetResources.yaml`                     |                                |                                     |
| Planetary schematics          | `/schematics`       | `planetSchematics.yaml`                    | `universe/schematics.yaml`     |                                     |
| Planets                       |                     | `mapPlanets.yaml`                          | `universe/planets.yaml`        |                                     |
| Proximity traps               |                     | `proximityTrap.yaml`                       |                                |                                     |
| Races                         |                     | `races.yaml`                               | `universe/races.yaml`          |                                     |
| Regions                       | `/regions`          | `mapRegions.yaml`                          | `universe/regions.yaml`        |                                     |
| Reprocessing                  | `/types`            | `typeMaterials.yaml`                       |                                | `typematerials.json`                |
| School map                    |                     | `schoolMap.yaml`                           |                                | `schoolmap.json`                    |
| Schools                       |                     | `schools.yaml`                             |                                | `schools.json`                      |
| Secondary suns                |                     | `mapSecondarySuns.yaml`                    |                                |                                     |
| Ship tree elements            |                     | `shipTreeElements.yaml`                    |                                |                                     |
| Ship tree factions            |                     | `shipTreeFactions.yaml`                    |                                |                                     |
| Ship tree groups              |                     | `shipTreeGroups.yaml`                      |                                |                                     |
| Skill plans                   |                     | `skillPlans.yaml`                          |                                | `skillplans.json`                   |
| Skills                        | `/skills `          | _types and dogma_                          | _types and dogma_              |                                     |
| Skin licenses                 |                     | `skinLicenses.yaml`                        |                                |                                     |
| Skin material names           |                     |                                            |                                | `skinmaterialnames.json`            |
| Skin materials                |                     | `skinMaterials.yaml`                       |                                | `skinmaterials.json`                |
| Skinr component categories    |                     | `skinrComponentCategories.yaml`            |                                |                                     |
| Skinr component point values  |                     | `skinrComponentPointValues.yaml`           |                                |                                     |
| Skinr component rarities      |                     | `skinrComponentRarities.yaml`              |                                |                                     |
| Skinr components              |                     | `skinrComponents.yaml`                     |                                |                                     |
| Skinr slot categories         |                     | `skinrSlotCategories.yaml`                 |                                |                                     |
| Skinr slot configurations     |                     | `skinrSlotConfigurations.yaml`             |                                |                                     |
| Skinr slot names              |                     | `skinrSlotNames.yaml`                      |                                |                                     |
| Skinr slots                   |                     | `skinrSlots.yaml`                          |                                |                                     |
| Skinr slots to materials      |                     | `skinrSlotsToMaterials.yaml`               |                                |                                     |
| Skinr tier thresholds         |                     | `skinrTierThresholds.yaml`                 |                                |                                     |
| Skins                         |                     | `skins.yaml`                               |                                | `skins.json`                        |
| Sovereignty upgrades          |                     | `sovereigntyUpgrades.yaml`                 |                                |                                     |
| Stargate                      |                     | `mapStargates.yaml`                        | `universe/stargates.yaml`      |                                     |
| Stars                         |                     | `mapStars.yaml`                            | `universe/stars.yaml`          |                                     |
| Station operation             |                     | `stationOperations.yaml`                   |                                |                                     |
| Station services              |                     | `stationServices.yaml`                     |                                |                                     |
| Station standing restrictions |                     | `stationStandingsRestrictions.yaml`        |                                | `stationstandingsrestrictions.json` |
| Stations                      |                     | `npcStations.yaml`                         | `universe/stations.yaml`       |                                     |
| System dbuff emitters         |                     | `systemDbuffEmitters.yaml`                 |                                |                                     |
| System wide effects           |                     | `systemWideEffects.yaml`                   |                                |                                     |
| Systems                       |                     | `mapSolarSystems.yaml`                     | `universe/systems.yaml`        |                                     |
| Tournament rule sets          |                     | `fsd/tournamentRuleSets.yaml` in old files |                                |                                     |
| Type elements                 |                     | `typeElements.yaml`                        |                                |                                     |
| Type lists                    |                     | `typeLists.yaml`                           |                                |                                     |

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
