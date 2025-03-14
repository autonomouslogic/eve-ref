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
/**
 * 
 * @export
 * @interface ModifierInfo
 */
export interface ModifierInfo {
    /**
     * 
     * @type {string}
     * @memberof ModifierInfo
     */
    domain?: string;
    /**
     * 
     * @type {number}
     * @memberof ModifierInfo
     */
    effectId?: number;
    /**
     * 
     * @type {string}
     * @memberof ModifierInfo
     */
    func?: string;
    /**
     * 
     * @type {number}
     * @memberof ModifierInfo
     */
    groupId?: number;
    /**
     * 
     * @type {number}
     * @memberof ModifierInfo
     */
    modifiedAttributeId?: number;
    /**
     * 
     * @type {number}
     * @memberof ModifierInfo
     */
    modifyingAttributeId?: number;
    /**
     * 
     * @type {number}
     * @memberof ModifierInfo
     */
    operator?: number;
    /**
     * 
     * @type {number}
     * @memberof ModifierInfo
     */
    skillTypeId?: number;
}

/**
 * Check if a given object implements the ModifierInfo interface.
 */
export function instanceOfModifierInfo(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function ModifierInfoFromJSON(json: any): ModifierInfo {
    return ModifierInfoFromJSONTyped(json, false);
}

export function ModifierInfoFromJSONTyped(json: any, ignoreDiscriminator: boolean): ModifierInfo {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'domain': !exists(json, 'domain') ? undefined : json['domain'],
        'effectId': !exists(json, 'effect_id') ? undefined : json['effect_id'],
        'func': !exists(json, 'func') ? undefined : json['func'],
        'groupId': !exists(json, 'group_id') ? undefined : json['group_id'],
        'modifiedAttributeId': !exists(json, 'modified_attribute_id') ? undefined : json['modified_attribute_id'],
        'modifyingAttributeId': !exists(json, 'modifying_attribute_id') ? undefined : json['modifying_attribute_id'],
        'operator': !exists(json, 'operator') ? undefined : json['operator'],
        'skillTypeId': !exists(json, 'skill_type_id') ? undefined : json['skill_type_id'],
    };
}

export function ModifierInfoToJSON(value?: ModifierInfo | null): any {
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
        'group_id': value.groupId,
        'modified_attribute_id': value.modifiedAttributeId,
        'modifying_attribute_id': value.modifyingAttributeId,
        'operator': value.operator,
        'skill_type_id': value.skillTypeId,
    };
}

