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
 * A meta group
 * @export
 * @interface MetaGroup
 */
export interface MetaGroup {
    /**
     * 
     * @type {number}
     * @memberof MetaGroup
     */
    metaGroupId?: number;
    /**
     * 
     * @type {number}
     * @memberof MetaGroup
     */
    iconId?: number;
    /**
     * 
     * @type {string}
     * @memberof MetaGroup
     */
    iconSuffix?: string;
    /**
     * 
     * @type {{ [key: string]: string; }}
     * @memberof MetaGroup
     */
    name?: { [key: string]: string; };
    /**
     * 
     * @type {{ [key: string]: string; }}
     * @memberof MetaGroup
     */
    description?: { [key: string]: string; };
    /**
     * The type IDs in this meta group. This is added by EVE Ref.
     * @type {Array<number>}
     * @memberof MetaGroup
     */
    typeIds?: Array<number>;
    /**
     * 
     * @type {Array<number>}
     * @memberof MetaGroup
     */
    color?: Array<number>;
}

/**
 * Check if a given object implements the MetaGroup interface.
 */
export function instanceOfMetaGroup(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function MetaGroupFromJSON(json: any): MetaGroup {
    return MetaGroupFromJSONTyped(json, false);
}

export function MetaGroupFromJSONTyped(json: any, ignoreDiscriminator: boolean): MetaGroup {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'metaGroupId': !exists(json, 'meta_group_id') ? undefined : json['meta_group_id'],
        'iconId': !exists(json, 'icon_id') ? undefined : json['icon_id'],
        'iconSuffix': !exists(json, 'icon_suffix') ? undefined : json['icon_suffix'],
        'name': !exists(json, 'name') ? undefined : json['name'],
        'description': !exists(json, 'description') ? undefined : json['description'],
        'typeIds': !exists(json, 'type_ids') ? undefined : json['type_ids'],
        'color': !exists(json, 'color') ? undefined : json['color'],
    };
}

export function MetaGroupToJSON(value?: MetaGroup | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'meta_group_id': value.metaGroupId,
        'icon_id': value.iconId,
        'icon_suffix': value.iconSuffix,
        'name': value.name,
        'description': value.description,
        'type_ids': value.typeIds,
        'color': value.color,
    };
}

