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
import type { ModifierInfo } from './ModifierInfo';
import {
    ModifierInfoFromJSON,
    ModifierInfoFromJSONTyped,
    ModifierInfoToJSON,
} from './ModifierInfo';

/**
 * 
 * @export
 * @interface DogmaEffect
 */
export interface DogmaEffect {
    /**
     * 
     * @type {number}
     * @memberof DogmaEffect
     */
    effectId?: number;
    /**
     * 
     * @type {string}
     * @memberof DogmaEffect
     */
    name?: string;
    /**
     * 
     * @type {{ [key: string]: string; }}
     * @memberof DogmaEffect
     */
    displayName?: { [key: string]: string; };
    /**
     * 
     * @type {{ [key: string]: string; }}
     * @memberof DogmaEffect
     */
    description?: { [key: string]: string; };
    /**
     * 
     * @type {boolean}
     * @memberof DogmaEffect
     */
    disallowAutoRepeat?: boolean;
    /**
     * 
     * @type {number}
     * @memberof DogmaEffect
     */
    effectCategory?: number;
    /**
     * 
     * @type {string}
     * @memberof DogmaEffect
     */
    effectName?: string;
    /**
     * 
     * @type {boolean}
     * @memberof DogmaEffect
     */
    electronicChance?: boolean;
    /**
     * 
     * @type {string}
     * @memberof DogmaEffect
     */
    guid?: string;
    /**
     * 
     * @type {number}
     * @memberof DogmaEffect
     */
    dischargeAttributeId?: number;
    /**
     * 
     * @type {number}
     * @memberof DogmaEffect
     */
    durationAttributeId?: number;
    /**
     * 
     * @type {number}
     * @memberof DogmaEffect
     */
    falloffAttributeId?: number;
    /**
     * 
     * @type {number}
     * @memberof DogmaEffect
     */
    rangeAttributeId?: number;
    /**
     * 
     * @type {number}
     * @memberof DogmaEffect
     */
    npcUsageChanceAttributeId?: number;
    /**
     * 
     * @type {number}
     * @memberof DogmaEffect
     */
    trackingSpeedAttributeId?: number;
    /**
     * 
     * @type {number}
     * @memberof DogmaEffect
     */
    npcActivationChanceAttributeId?: number;
    /**
     * 
     * @type {number}
     * @memberof DogmaEffect
     */
    fittingUsageChanceAttributeId?: number;
    /**
     * 
     * @type {number}
     * @memberof DogmaEffect
     */
    resistanceAttributeId?: number;
    /**
     * 
     * @type {number}
     * @memberof DogmaEffect
     */
    distribution?: number;
    /**
     * 
     * @type {number}
     * @memberof DogmaEffect
     */
    iconId?: number;
    /**
     * 
     * @type {boolean}
     * @memberof DogmaEffect
     */
    isAssistance?: boolean;
    /**
     * 
     * @type {boolean}
     * @memberof DogmaEffect
     */
    isOffensive?: boolean;
    /**
     * 
     * @type {boolean}
     * @memberof DogmaEffect
     */
    isWarpSafe?: boolean;
    /**
     * 
     * @type {boolean}
     * @memberof DogmaEffect
     */
    propulsionChance?: boolean;
    /**
     * 
     * @type {boolean}
     * @memberof DogmaEffect
     */
    published?: boolean;
    /**
     * 
     * @type {boolean}
     * @memberof DogmaEffect
     */
    rangeChance?: boolean;
    /**
     * 
     * @type {string}
     * @memberof DogmaEffect
     */
    sfxName?: string;
    /**
     * 
     * @type {Array<ModifierInfo>}
     * @memberof DogmaEffect
     */
    modifiers?: Array<ModifierInfo>;
}

/**
 * Check if a given object implements the DogmaEffect interface.
 */
export function instanceOfDogmaEffect(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function DogmaEffectFromJSON(json: any): DogmaEffect {
    return DogmaEffectFromJSONTyped(json, false);
}

export function DogmaEffectFromJSONTyped(json: any, ignoreDiscriminator: boolean): DogmaEffect {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'effectId': !exists(json, 'effect_id') ? undefined : json['effect_id'],
        'name': !exists(json, 'name') ? undefined : json['name'],
        'displayName': !exists(json, 'display_name') ? undefined : json['display_name'],
        'description': !exists(json, 'description') ? undefined : json['description'],
        'disallowAutoRepeat': !exists(json, 'disallow_auto_repeat') ? undefined : json['disallow_auto_repeat'],
        'effectCategory': !exists(json, 'effect_category') ? undefined : json['effect_category'],
        'effectName': !exists(json, 'effect_name') ? undefined : json['effect_name'],
        'electronicChance': !exists(json, 'electronic_chance') ? undefined : json['electronic_chance'],
        'guid': !exists(json, 'guid') ? undefined : json['guid'],
        'dischargeAttributeId': !exists(json, 'discharge_attribute_id') ? undefined : json['discharge_attribute_id'],
        'durationAttributeId': !exists(json, 'duration_attribute_id') ? undefined : json['duration_attribute_id'],
        'falloffAttributeId': !exists(json, 'falloff_attribute_id') ? undefined : json['falloff_attribute_id'],
        'rangeAttributeId': !exists(json, 'range_attribute_id') ? undefined : json['range_attribute_id'],
        'npcUsageChanceAttributeId': !exists(json, 'npc_usage_chance_attribute_id') ? undefined : json['npc_usage_chance_attribute_id'],
        'trackingSpeedAttributeId': !exists(json, 'tracking_speed_attribute_id') ? undefined : json['tracking_speed_attribute_id'],
        'npcActivationChanceAttributeId': !exists(json, 'npc_activation_chance_attribute_id') ? undefined : json['npc_activation_chance_attribute_id'],
        'fittingUsageChanceAttributeId': !exists(json, 'fitting_usage_chance_attribute_id') ? undefined : json['fitting_usage_chance_attribute_id'],
        'resistanceAttributeId': !exists(json, 'resistance_attribute_id') ? undefined : json['resistance_attribute_id'],
        'distribution': !exists(json, 'distribution') ? undefined : json['distribution'],
        'iconId': !exists(json, 'icon_id') ? undefined : json['icon_id'],
        'isAssistance': !exists(json, 'is_assistance') ? undefined : json['is_assistance'],
        'isOffensive': !exists(json, 'is_offensive') ? undefined : json['is_offensive'],
        'isWarpSafe': !exists(json, 'is_warp_safe') ? undefined : json['is_warp_safe'],
        'propulsionChance': !exists(json, 'propulsion_chance') ? undefined : json['propulsion_chance'],
        'published': !exists(json, 'published') ? undefined : json['published'],
        'rangeChance': !exists(json, 'range_chance') ? undefined : json['range_chance'],
        'sfxName': !exists(json, 'sfx_name') ? undefined : json['sfx_name'],
        'modifiers': !exists(json, 'modifiers') ? undefined : ((json['modifiers'] as Array<any>).map(ModifierInfoFromJSON)),
    };
}

export function DogmaEffectToJSON(value?: DogmaEffect | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'effect_id': value.effectId,
        'name': value.name,
        'display_name': value.displayName,
        'description': value.description,
        'disallow_auto_repeat': value.disallowAutoRepeat,
        'effect_category': value.effectCategory,
        'effect_name': value.effectName,
        'electronic_chance': value.electronicChance,
        'guid': value.guid,
        'discharge_attribute_id': value.dischargeAttributeId,
        'duration_attribute_id': value.durationAttributeId,
        'falloff_attribute_id': value.falloffAttributeId,
        'range_attribute_id': value.rangeAttributeId,
        'npc_usage_chance_attribute_id': value.npcUsageChanceAttributeId,
        'tracking_speed_attribute_id': value.trackingSpeedAttributeId,
        'npc_activation_chance_attribute_id': value.npcActivationChanceAttributeId,
        'fitting_usage_chance_attribute_id': value.fittingUsageChanceAttributeId,
        'resistance_attribute_id': value.resistanceAttributeId,
        'distribution': value.distribution,
        'icon_id': value.iconId,
        'is_assistance': value.isAssistance,
        'is_offensive': value.isOffensive,
        'is_warp_safe': value.isWarpSafe,
        'propulsion_chance': value.propulsionChance,
        'published': value.published,
        'range_chance': value.rangeChance,
        'sfx_name': value.sfxName,
        'modifiers': value.modifiers === undefined ? undefined : ((value.modifiers as Array<any>).map(ModifierInfoToJSON)),
    };
}

