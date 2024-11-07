/* tslint:disable */
/* eslint-disable */
/**
 * EVE Swagger Interface
 * An OpenAPI for EVE Online
 *
 * The version of the OpenAPI document: 1.19
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { exists, mapValues } from '../runtime';
import type { GetDogmaEffectsEffectIdModifier } from './GetDogmaEffectsEffectIdModifier';
import {
    GetDogmaEffectsEffectIdModifierFromJSON,
    GetDogmaEffectsEffectIdModifierFromJSONTyped,
    GetDogmaEffectsEffectIdModifierToJSON,
} from './GetDogmaEffectsEffectIdModifier';

/**
 * 200 ok object
 * @export
 * @interface GetDogmaEffectsEffectIdOk
 */
export interface GetDogmaEffectsEffectIdOk {
    /**
     * description string
     * @type {string}
     * @memberof GetDogmaEffectsEffectIdOk
     */
    description?: string;
    /**
     * disallow_auto_repeat boolean
     * @type {boolean}
     * @memberof GetDogmaEffectsEffectIdOk
     */
    disallowAutoRepeat?: boolean;
    /**
     * discharge_attribute_id integer
     * @type {number}
     * @memberof GetDogmaEffectsEffectIdOk
     */
    dischargeAttributeId?: number;
    /**
     * display_name string
     * @type {string}
     * @memberof GetDogmaEffectsEffectIdOk
     */
    displayName?: string;
    /**
     * duration_attribute_id integer
     * @type {number}
     * @memberof GetDogmaEffectsEffectIdOk
     */
    durationAttributeId?: number;
    /**
     * effect_category integer
     * @type {number}
     * @memberof GetDogmaEffectsEffectIdOk
     */
    effectCategory?: number;
    /**
     * effect_id integer
     * @type {number}
     * @memberof GetDogmaEffectsEffectIdOk
     */
    effectId: number;
    /**
     * electronic_chance boolean
     * @type {boolean}
     * @memberof GetDogmaEffectsEffectIdOk
     */
    electronicChance?: boolean;
    /**
     * falloff_attribute_id integer
     * @type {number}
     * @memberof GetDogmaEffectsEffectIdOk
     */
    falloffAttributeId?: number;
    /**
     * icon_id integer
     * @type {number}
     * @memberof GetDogmaEffectsEffectIdOk
     */
    iconId?: number;
    /**
     * is_assistance boolean
     * @type {boolean}
     * @memberof GetDogmaEffectsEffectIdOk
     */
    isAssistance?: boolean;
    /**
     * is_offensive boolean
     * @type {boolean}
     * @memberof GetDogmaEffectsEffectIdOk
     */
    isOffensive?: boolean;
    /**
     * is_warp_safe boolean
     * @type {boolean}
     * @memberof GetDogmaEffectsEffectIdOk
     */
    isWarpSafe?: boolean;
    /**
     * modifiers array
     * @type {Array<GetDogmaEffectsEffectIdModifier>}
     * @memberof GetDogmaEffectsEffectIdOk
     */
    modifiers?: Array<GetDogmaEffectsEffectIdModifier>;
    /**
     * name string
     * @type {string}
     * @memberof GetDogmaEffectsEffectIdOk
     */
    name?: string;
    /**
     * post_expression integer
     * @type {number}
     * @memberof GetDogmaEffectsEffectIdOk
     */
    postExpression?: number;
    /**
     * pre_expression integer
     * @type {number}
     * @memberof GetDogmaEffectsEffectIdOk
     */
    preExpression?: number;
    /**
     * published boolean
     * @type {boolean}
     * @memberof GetDogmaEffectsEffectIdOk
     */
    published?: boolean;
    /**
     * range_attribute_id integer
     * @type {number}
     * @memberof GetDogmaEffectsEffectIdOk
     */
    rangeAttributeId?: number;
    /**
     * range_chance boolean
     * @type {boolean}
     * @memberof GetDogmaEffectsEffectIdOk
     */
    rangeChance?: boolean;
    /**
     * tracking_speed_attribute_id integer
     * @type {number}
     * @memberof GetDogmaEffectsEffectIdOk
     */
    trackingSpeedAttributeId?: number;
}

/**
 * Check if a given object implements the GetDogmaEffectsEffectIdOk interface.
 */
export function instanceOfGetDogmaEffectsEffectIdOk(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "effectId" in value;

    return isInstance;
}

export function GetDogmaEffectsEffectIdOkFromJSON(json: any): GetDogmaEffectsEffectIdOk {
    return GetDogmaEffectsEffectIdOkFromJSONTyped(json, false);
}

export function GetDogmaEffectsEffectIdOkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetDogmaEffectsEffectIdOk {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'description': !exists(json, 'description') ? undefined : json['description'],
        'disallowAutoRepeat': !exists(json, 'disallow_auto_repeat') ? undefined : json['disallow_auto_repeat'],
        'dischargeAttributeId': !exists(json, 'discharge_attribute_id') ? undefined : json['discharge_attribute_id'],
        'displayName': !exists(json, 'display_name') ? undefined : json['display_name'],
        'durationAttributeId': !exists(json, 'duration_attribute_id') ? undefined : json['duration_attribute_id'],
        'effectCategory': !exists(json, 'effect_category') ? undefined : json['effect_category'],
        'effectId': json['effect_id'],
        'electronicChance': !exists(json, 'electronic_chance') ? undefined : json['electronic_chance'],
        'falloffAttributeId': !exists(json, 'falloff_attribute_id') ? undefined : json['falloff_attribute_id'],
        'iconId': !exists(json, 'icon_id') ? undefined : json['icon_id'],
        'isAssistance': !exists(json, 'is_assistance') ? undefined : json['is_assistance'],
        'isOffensive': !exists(json, 'is_offensive') ? undefined : json['is_offensive'],
        'isWarpSafe': !exists(json, 'is_warp_safe') ? undefined : json['is_warp_safe'],
        'modifiers': !exists(json, 'modifiers') ? undefined : ((json['modifiers'] as Array<any>).map(GetDogmaEffectsEffectIdModifierFromJSON)),
        'name': !exists(json, 'name') ? undefined : json['name'],
        'postExpression': !exists(json, 'post_expression') ? undefined : json['post_expression'],
        'preExpression': !exists(json, 'pre_expression') ? undefined : json['pre_expression'],
        'published': !exists(json, 'published') ? undefined : json['published'],
        'rangeAttributeId': !exists(json, 'range_attribute_id') ? undefined : json['range_attribute_id'],
        'rangeChance': !exists(json, 'range_chance') ? undefined : json['range_chance'],
        'trackingSpeedAttributeId': !exists(json, 'tracking_speed_attribute_id') ? undefined : json['tracking_speed_attribute_id'],
    };
}

export function GetDogmaEffectsEffectIdOkToJSON(value?: GetDogmaEffectsEffectIdOk | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'description': value.description,
        'disallow_auto_repeat': value.disallowAutoRepeat,
        'discharge_attribute_id': value.dischargeAttributeId,
        'display_name': value.displayName,
        'duration_attribute_id': value.durationAttributeId,
        'effect_category': value.effectCategory,
        'effect_id': value.effectId,
        'electronic_chance': value.electronicChance,
        'falloff_attribute_id': value.falloffAttributeId,
        'icon_id': value.iconId,
        'is_assistance': value.isAssistance,
        'is_offensive': value.isOffensive,
        'is_warp_safe': value.isWarpSafe,
        'modifiers': value.modifiers === undefined ? undefined : ((value.modifiers as Array<any>).map(GetDogmaEffectsEffectIdModifierToJSON)),
        'name': value.name,
        'post_expression': value.postExpression,
        'pre_expression': value.preExpression,
        'published': value.published,
        'range_attribute_id': value.rangeAttributeId,
        'range_chance': value.rangeChance,
        'tracking_speed_attribute_id': value.trackingSpeedAttributeId,
    };
}

