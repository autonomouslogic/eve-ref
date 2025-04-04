/* tslint:disable */
/* eslint-disable */
/**
 * EVE Swagger Interface
 * An OpenAPI for EVE Online
 *
 * The version of the OpenAPI document: 1.25
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { exists, mapValues } from '../runtime';
/**
 * modifier object
 * @export
 * @interface GetDogmaEffectsEffectIdModifier
 */
export interface GetDogmaEffectsEffectIdModifier {
    /**
     * domain string
     * @type {string}
     * @memberof GetDogmaEffectsEffectIdModifier
     */
    domain?: string;
    /**
     * effect_id integer
     * @type {number}
     * @memberof GetDogmaEffectsEffectIdModifier
     */
    effectId?: number;
    /**
     * func string
     * @type {string}
     * @memberof GetDogmaEffectsEffectIdModifier
     */
    func: string;
    /**
     * modified_attribute_id integer
     * @type {number}
     * @memberof GetDogmaEffectsEffectIdModifier
     */
    modifiedAttributeId?: number;
    /**
     * modifying_attribute_id integer
     * @type {number}
     * @memberof GetDogmaEffectsEffectIdModifier
     */
    modifyingAttributeId?: number;
    /**
     * operator integer
     * @type {number}
     * @memberof GetDogmaEffectsEffectIdModifier
     */
    operator?: number;
}

/**
 * Check if a given object implements the GetDogmaEffectsEffectIdModifier interface.
 */
export function instanceOfGetDogmaEffectsEffectIdModifier(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "func" in value;

    return isInstance;
}

export function GetDogmaEffectsEffectIdModifierFromJSON(json: any): GetDogmaEffectsEffectIdModifier {
    return GetDogmaEffectsEffectIdModifierFromJSONTyped(json, false);
}

export function GetDogmaEffectsEffectIdModifierFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetDogmaEffectsEffectIdModifier {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'domain': !exists(json, 'domain') ? undefined : json['domain'],
        'effectId': !exists(json, 'effect_id') ? undefined : json['effect_id'],
        'func': json['func'],
        'modifiedAttributeId': !exists(json, 'modified_attribute_id') ? undefined : json['modified_attribute_id'],
        'modifyingAttributeId': !exists(json, 'modifying_attribute_id') ? undefined : json['modifying_attribute_id'],
        'operator': !exists(json, 'operator') ? undefined : json['operator'],
    };
}

export function GetDogmaEffectsEffectIdModifierToJSON(value?: GetDogmaEffectsEffectIdModifier | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'domain': value.domain,
        'effect_id': value.effectId,
        'func': value.func,
        'modified_attribute_id': value.modifiedAttributeId,
        'modifying_attribute_id': value.modifyingAttributeId,
        'operator': value.operator,
    };
}

