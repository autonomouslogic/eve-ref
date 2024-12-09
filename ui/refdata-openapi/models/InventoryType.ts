/* tslint:disable */
/* eslint-disable */
/**
 * EVE Ref Reference Data for EVE Online
 * This spec should be considered unstable and subject to change at any time.
 *
 * The version of the OpenAPI document: dev
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { exists, mapValues } from '../runtime';
import type { DogmaTypeAttribute } from './DogmaTypeAttribute';
import {
    DogmaTypeAttributeFromJSON,
    DogmaTypeAttributeFromJSONTyped,
    DogmaTypeAttributeToJSON,
} from './DogmaTypeAttribute';
import type { DogmaTypeEffect } from './DogmaTypeEffect';
import {
    DogmaTypeEffectFromJSON,
    DogmaTypeEffectFromJSONTyped,
    DogmaTypeEffectToJSON,
} from './DogmaTypeEffect';
import type { InventoryTypeTraits } from './InventoryTypeTraits';
import {
    InventoryTypeTraitsFromJSON,
    InventoryTypeTraitsFromJSONTyped,
    InventoryTypeTraitsToJSON,
} from './InventoryTypeTraits';
import type { ProducingBlueprint } from './ProducingBlueprint';
import {
    ProducingBlueprintFromJSON,
    ProducingBlueprintFromJSONTyped,
    ProducingBlueprintToJSON,
} from './ProducingBlueprint';
import type { TypeMaterial } from './TypeMaterial';
import {
    TypeMaterialFromJSON,
    TypeMaterialFromJSONTyped,
    TypeMaterialToJSON,
} from './TypeMaterial';
import type { UsedInBlueprint } from './UsedInBlueprint';
import {
    UsedInBlueprintFromJSON,
    UsedInBlueprintFromJSONTyped,
    UsedInBlueprintToJSON,
} from './UsedInBlueprint';

/**
 * An inventory type
 * @export
 * @interface InventoryType
 */
export interface InventoryType {
    /**
     * 
     * @type {number}
     * @memberof InventoryType
     */
    typeId?: number;
    /**
     * 
     * @type {number}
     * @memberof InventoryType
     */
    basePrice?: number;
    /**
     * 
     * @type {number}
     * @memberof InventoryType
     */
    capacity?: number;
    /**
     * The key is the language code.
     * @type {{ [key: string]: string; }}
     * @memberof InventoryType
     */
    description?: { [key: string]: string; };
    /**
     * A map of dogma attributes. The key is the attribute ID
     * @type {{ [key: string]: DogmaTypeAttribute; }}
     * @memberof InventoryType
     */
    dogmaAttributes?: { [key: string]: DogmaTypeAttribute; };
    /**
     * A map of dogma attributes. The key is the attribute ID
     * @type {{ [key: string]: DogmaTypeEffect; }}
     * @memberof InventoryType
     */
    dogmaEffects?: { [key: string]: DogmaTypeEffect; };
    /**
     * 
     * @type {number}
     * @memberof InventoryType
     */
    factionId?: number;
    /**
     * 
     * @type {number}
     * @memberof InventoryType
     */
    graphicId?: number;
    /**
     * The category ID the group is in. This is added by EVE Ref.
     * @type {number}
     * @memberof InventoryType
     */
    categoryId?: number;
    /**
     * 
     * @type {number}
     * @memberof InventoryType
     */
    groupId?: number;
    /**
     * 
     * @type {number}
     * @memberof InventoryType
     */
    iconId?: number;
    /**
     * 
     * @type {number}
     * @memberof InventoryType
     */
    marketGroupId?: number;
    /**
     * 
     * @type {number}
     * @memberof InventoryType
     */
    mass?: number;
    /**
     * 
     * @type {{ [key: string]: Array<number>; }}
     * @memberof InventoryType
     */
    masteries?: { [key: string]: Array<number>; };
    /**
     * 
     * @type {number}
     * @memberof InventoryType
     */
    metaGroupId?: number;
    /**
     * The key is the language code.
     * @type {{ [key: string]: string; }}
     * @memberof InventoryType
     */
    name?: { [key: string]: string; };
    /**
     * 
     * @type {number}
     * @memberof InventoryType
     */
    packagedVolume?: number;
    /**
     * 
     * @type {number}
     * @memberof InventoryType
     */
    portionSize?: number;
    /**
     * 
     * @type {boolean}
     * @memberof InventoryType
     */
    published?: boolean;
    /**
     * 
     * @type {number}
     * @memberof InventoryType
     */
    raceId?: number;
    /**
     * 
     * @type {number}
     * @memberof InventoryType
     */
    radius?: number;
    /**
     * 
     * @type {string}
     * @memberof InventoryType
     */
    sofFactionName?: string;
    /**
     * 
     * @type {number}
     * @memberof InventoryType
     */
    sofMaterialSetId?: number;
    /**
     * 
     * @type {number}
     * @memberof InventoryType
     */
    soundId?: number;
    /**
     * 
     * @type {InventoryTypeTraits}
     * @memberof InventoryType
     */
    traits?: InventoryTypeTraits;
    /**
     * 
     * @type {number}
     * @memberof InventoryType
     */
    variationParentTypeId?: number;
    /**
     * 
     * @type {number}
     * @memberof InventoryType
     */
    volume?: number;
    /**
     * The skills required for this type. The key is the skill type ID and the value is the level. This is added by EVE Ref and derived from dogma attributes.
     * @type {{ [key: string]: number; }}
     * @memberof InventoryType
     */
    requiredSkills?: { [key: string]: number; };
    /**
     * Which mutaplasmids can be applied to this type to create a dynamic item. This is added by EVE Ref.
     * @type {Array<number>}
     * @memberof InventoryType
     */
    applicableMutaplasmidTypeIds?: Array<number>;
    /**
     * Which mutaplasmids can used to create this dynamic item. This is added by EVE Ref.
     * @type {Array<number>}
     * @memberof InventoryType
     */
    creatingMutaplasmidTypeIds?: Array<number>;
    /**
     * The variations for this type. The key is the meta group and the value is a list of type IDs. This is added by EVE Ref.
     * @type {{ [key: string]: Array<number>; }}
     * @memberof InventoryType
     */
    typeVariations?: { [key: string]: Array<number>; };
    /**
     * The variations for this ore type. The key is the asteroid meta level and the value is a list of type IDs. This is added by EVE Ref.
     * @type {{ [key: string]: Array<number>; }}
     * @memberof InventoryType
     */
    oreVariations?: { [key: string]: Array<number>; };
    /**
     * Whether this is an ore or not. This is added by EVE Ref.
     * @type {boolean}
     * @memberof InventoryType
     */
    isOre?: boolean;
    /**
     * The blueprints producing this type. The key is the blueprint type ID. This is added by EVE Ref.
     * @type {{ [key: string]: ProducingBlueprint; }}
     * @memberof InventoryType
     */
    producedByBlueprints?: { [key: string]: ProducingBlueprint; };
    /**
     * 
     * @type {{ [key: string]: TypeMaterial; }}
     * @memberof InventoryType
     */
    typeMaterials?: { [key: string]: TypeMaterial; };
    /**
     * Types this can be fitted to. This is added by EVE Ref.
     * @type {Array<number>}
     * @memberof InventoryType
     */
    canFitTypes?: Array<number>;
    /**
     * Types which can be fitted. This is added by EVE Ref.
     * @type {Array<number>}
     * @memberof InventoryType
     */
    canBeFittedWithTypes?: Array<number>;
    /**
     * The schematics consuming this type. This is added by EVE Ref.
     * @type {Array<number>}
     * @memberof InventoryType
     */
    usedBySchematicIds?: Array<number>;
    /**
     * The schematics producing this type. This is added by EVE Ref.
     * @type {Array<number>}
     * @memberof InventoryType
     */
    producedBySchematicIds?: Array<number>;
    /**
     * The type IDs for the planetary extractor pins which can be used to harvest this type. This is added by EVE Ref.
     * @type {Array<number>}
     * @memberof InventoryType
     */
    harvestedByPinTypeIds?: Array<number>;
    /**
     * The type IDs for the planetary pins which can be built on this planet. This is added by EVE Ref.
     * @type {Array<number>}
     * @memberof InventoryType
     */
    buildablePinTypeIds?: Array<number>;
    /**
     * The schematic IDs which can be installed into this planetary processor. This is added by EVE Ref.
     * @type {Array<number>}
     * @memberof InventoryType
     */
    installableSchematicIds?: Array<number>;
    /**
     * The blueprints in which this type is used. The first key is the blueprint ID and the second key is the activity name. This is added by EVE Ref.
     * @type {{ [key: string]: { [key: string]: UsedInBlueprint; }; }}
     * @memberof InventoryType
     */
    usedInBlueprints?: { [key: string]: { [key: string]: UsedInBlueprint; }; };
    /**
     * For structure engineering rigs, these are the category IDs the rig affects in some way. This is added by EVE Ref.
     * @type {Array<number>}
     * @memberof InventoryType
     */
    engineeringRigAffectedCategoryIds?: Array<number>;
    /**
     * For structure engineering rigs, these are the group IDs the rig affects in some way. This is added by EVE Ref.
     * @type {Array<number>}
     * @memberof InventoryType
     */
    engineeringRigAffectedGroupIds?: Array<number>;
    /**
     * These are the type IDs of the engineering rigs which affect this type in some way. This is added by EVE Ref.
     * @type {Array<number>}
     * @memberof InventoryType
     */
    engineeringRigSourceTypeIds?: Array<number>;
    /**
     * Whether this type is a skill or not. This is added by EVE Ref.
     * @type {boolean}
     * @memberof InventoryType
     */
    isSkill?: boolean;
    /**
     * Whether this type is a mutaplasmid or not. This is added by EVE Ref.
     * @type {boolean}
     * @memberof InventoryType
     */
    isMutaplasmid?: boolean;
    /**
     * Whether this type is a dynamic item created by a mutaplasmid or not. This is added by EVE Ref.
     * @type {boolean}
     * @memberof InventoryType
     */
    isDynamicItem?: boolean;
    /**
     * Whether this type is a blueprint or not. This is added by EVE Ref.
     * @type {boolean}
     * @memberof InventoryType
     */
    isBlueprint?: boolean;
}

/**
 * Check if a given object implements the InventoryType interface.
 */
export function instanceOfInventoryType(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function InventoryTypeFromJSON(json: any): InventoryType {
    return InventoryTypeFromJSONTyped(json, false);
}

export function InventoryTypeFromJSONTyped(json: any, ignoreDiscriminator: boolean): InventoryType {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'typeId': !exists(json, 'type_id') ? undefined : json['type_id'],
        'basePrice': !exists(json, 'base_price') ? undefined : json['base_price'],
        'capacity': !exists(json, 'capacity') ? undefined : json['capacity'],
        'description': !exists(json, 'description') ? undefined : json['description'],
        'dogmaAttributes': !exists(json, 'dogma_attributes') ? undefined : (mapValues(json['dogma_attributes'], DogmaTypeAttributeFromJSON)),
        'dogmaEffects': !exists(json, 'dogma_effects') ? undefined : (mapValues(json['dogma_effects'], DogmaTypeEffectFromJSON)),
        'factionId': !exists(json, 'faction_id') ? undefined : json['faction_id'],
        'graphicId': !exists(json, 'graphic_id') ? undefined : json['graphic_id'],
        'categoryId': !exists(json, 'category_id') ? undefined : json['category_id'],
        'groupId': !exists(json, 'group_id') ? undefined : json['group_id'],
        'iconId': !exists(json, 'icon_id') ? undefined : json['icon_id'],
        'marketGroupId': !exists(json, 'market_group_id') ? undefined : json['market_group_id'],
        'mass': !exists(json, 'mass') ? undefined : json['mass'],
        'masteries': !exists(json, 'masteries') ? undefined : json['masteries'],
        'metaGroupId': !exists(json, 'meta_group_id') ? undefined : json['meta_group_id'],
        'name': !exists(json, 'name') ? undefined : json['name'],
        'packagedVolume': !exists(json, 'packaged_volume') ? undefined : json['packaged_volume'],
        'portionSize': !exists(json, 'portion_size') ? undefined : json['portion_size'],
        'published': !exists(json, 'published') ? undefined : json['published'],
        'raceId': !exists(json, 'race_id') ? undefined : json['race_id'],
        'radius': !exists(json, 'radius') ? undefined : json['radius'],
        'sofFactionName': !exists(json, 'sof_faction_name') ? undefined : json['sof_faction_name'],
        'sofMaterialSetId': !exists(json, 'sof_material_set_id') ? undefined : json['sof_material_set_id'],
        'soundId': !exists(json, 'sound_id') ? undefined : json['sound_id'],
        'traits': !exists(json, 'traits') ? undefined : InventoryTypeTraitsFromJSON(json['traits']),
        'variationParentTypeId': !exists(json, 'variation_parent_type_id') ? undefined : json['variation_parent_type_id'],
        'volume': !exists(json, 'volume') ? undefined : json['volume'],
        'requiredSkills': !exists(json, 'required_skills') ? undefined : json['required_skills'],
        'applicableMutaplasmidTypeIds': !exists(json, 'applicable_mutaplasmid_type_ids') ? undefined : json['applicable_mutaplasmid_type_ids'],
        'creatingMutaplasmidTypeIds': !exists(json, 'creating_mutaplasmid_type_ids') ? undefined : json['creating_mutaplasmid_type_ids'],
        'typeVariations': !exists(json, 'type_variations') ? undefined : json['type_variations'],
        'oreVariations': !exists(json, 'ore_variations') ? undefined : json['ore_variations'],
        'isOre': !exists(json, 'is_ore') ? undefined : json['is_ore'],
        'producedByBlueprints': !exists(json, 'produced_by_blueprints') ? undefined : (mapValues(json['produced_by_blueprints'], ProducingBlueprintFromJSON)),
        'typeMaterials': !exists(json, 'type_materials') ? undefined : (mapValues(json['type_materials'], TypeMaterialFromJSON)),
        'canFitTypes': !exists(json, 'can_fit_types') ? undefined : json['can_fit_types'],
        'canBeFittedWithTypes': !exists(json, 'can_be_fitted_with_types') ? undefined : json['can_be_fitted_with_types'],
        'usedBySchematicIds': !exists(json, 'used_by_schematic_ids') ? undefined : json['used_by_schematic_ids'],
        'producedBySchematicIds': !exists(json, 'produced_by_schematic_ids') ? undefined : json['produced_by_schematic_ids'],
        'harvestedByPinTypeIds': !exists(json, 'harvested_by_pin_type_ids') ? undefined : json['harvested_by_pin_type_ids'],
        'buildablePinTypeIds': !exists(json, 'buildable_pin_type_ids') ? undefined : json['buildable_pin_type_ids'],
        'installableSchematicIds': !exists(json, 'installable_schematic_ids') ? undefined : json['installable_schematic_ids'],
        'usedInBlueprints': !exists(json, 'used_in_blueprints') ? undefined : json['used_in_blueprints'],
        'engineeringRigAffectedCategoryIds': !exists(json, 'engineering_rig_affected_category_ids') ? undefined : json['engineering_rig_affected_category_ids'],
        'engineeringRigAffectedGroupIds': !exists(json, 'engineering_rig_affected_group_ids') ? undefined : json['engineering_rig_affected_group_ids'],
        'engineeringRigSourceTypeIds': !exists(json, 'engineering_rig_source_type_ids') ? undefined : json['engineering_rig_source_type_ids'],
        'isSkill': !exists(json, 'is_skill') ? undefined : json['is_skill'],
        'isMutaplasmid': !exists(json, 'is_mutaplasmid') ? undefined : json['is_mutaplasmid'],
        'isDynamicItem': !exists(json, 'is_dynamic_item') ? undefined : json['is_dynamic_item'],
        'isBlueprint': !exists(json, 'is_blueprint') ? undefined : json['is_blueprint'],
    };
}

export function InventoryTypeToJSON(value?: InventoryType | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'type_id': value.typeId,
        'base_price': value.basePrice,
        'capacity': value.capacity,
        'description': value.description,
        'dogma_attributes': value.dogmaAttributes === undefined ? undefined : (mapValues(value.dogmaAttributes, DogmaTypeAttributeToJSON)),
        'dogma_effects': value.dogmaEffects === undefined ? undefined : (mapValues(value.dogmaEffects, DogmaTypeEffectToJSON)),
        'faction_id': value.factionId,
        'graphic_id': value.graphicId,
        'category_id': value.categoryId,
        'group_id': value.groupId,
        'icon_id': value.iconId,
        'market_group_id': value.marketGroupId,
        'mass': value.mass,
        'masteries': value.masteries,
        'meta_group_id': value.metaGroupId,
        'name': value.name,
        'packaged_volume': value.packagedVolume,
        'portion_size': value.portionSize,
        'published': value.published,
        'race_id': value.raceId,
        'radius': value.radius,
        'sof_faction_name': value.sofFactionName,
        'sof_material_set_id': value.sofMaterialSetId,
        'sound_id': value.soundId,
        'traits': InventoryTypeTraitsToJSON(value.traits),
        'variation_parent_type_id': value.variationParentTypeId,
        'volume': value.volume,
        'required_skills': value.requiredSkills,
        'applicable_mutaplasmid_type_ids': value.applicableMutaplasmidTypeIds,
        'creating_mutaplasmid_type_ids': value.creatingMutaplasmidTypeIds,
        'type_variations': value.typeVariations,
        'ore_variations': value.oreVariations,
        'is_ore': value.isOre,
        'produced_by_blueprints': value.producedByBlueprints === undefined ? undefined : (mapValues(value.producedByBlueprints, ProducingBlueprintToJSON)),
        'type_materials': value.typeMaterials === undefined ? undefined : (mapValues(value.typeMaterials, TypeMaterialToJSON)),
        'can_fit_types': value.canFitTypes,
        'can_be_fitted_with_types': value.canBeFittedWithTypes,
        'used_by_schematic_ids': value.usedBySchematicIds,
        'produced_by_schematic_ids': value.producedBySchematicIds,
        'harvested_by_pin_type_ids': value.harvestedByPinTypeIds,
        'buildable_pin_type_ids': value.buildablePinTypeIds,
        'installable_schematic_ids': value.installableSchematicIds,
        'used_in_blueprints': value.usedInBlueprints,
        'engineering_rig_affected_category_ids': value.engineeringRigAffectedCategoryIds,
        'engineering_rig_affected_group_ids': value.engineeringRigAffectedGroupIds,
        'engineering_rig_source_type_ids': value.engineeringRigSourceTypeIds,
        'is_skill': value.isSkill,
        'is_mutaplasmid': value.isMutaplasmid,
        'is_dynamic_item': value.isDynamicItem,
        'is_blueprint': value.isBlueprint,
    };
}

