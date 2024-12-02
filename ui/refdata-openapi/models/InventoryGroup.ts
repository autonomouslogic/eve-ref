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
 * An inventory group
 * @export
 * @interface InventoryGroup
 */
export interface InventoryGroup {
    /**
     * 
     * @type {number}
     * @memberof InventoryGroup
     */
    groupId?: number;
    /**
     * 
     * @type {number}
     * @memberof InventoryGroup
     */
    categoryId?: number;
    /**
     * 
     * @type {number}
     * @memberof InventoryGroup
     */
    iconId?: number;
    /**
     * 
     * @type {{ [key: string]: string; }}
     * @memberof InventoryGroup
     */
    name?: { [key: string]: string; };
    /**
     * 
     * @type {boolean}
     * @memberof InventoryGroup
     */
    anchorable?: boolean;
    /**
     * 
     * @type {boolean}
     * @memberof InventoryGroup
     */
    anchored?: boolean;
    /**
     * 
     * @type {boolean}
     * @memberof InventoryGroup
     */
    fittableNonSingleton?: boolean;
    /**
     * 
     * @type {boolean}
     * @memberof InventoryGroup
     */
    published?: boolean;
    /**
     * 
     * @type {boolean}
     * @memberof InventoryGroup
     */
    useBasePrice?: boolean;
    /**
     * The type IDs in this group. This is added by EVE Ref.
     * @type {Array<number>}
     * @memberof InventoryGroup
     */
    typeIds?: Array<number>;
}

/**
 * Check if a given object implements the InventoryGroup interface.
 */
export function instanceOfInventoryGroup(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function InventoryGroupFromJSON(json: any): InventoryGroup {
    return InventoryGroupFromJSONTyped(json, false);
}

export function InventoryGroupFromJSONTyped(json: any, ignoreDiscriminator: boolean): InventoryGroup {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'groupId': !exists(json, 'group_id') ? undefined : json['group_id'],
        'categoryId': !exists(json, 'category_id') ? undefined : json['category_id'],
        'iconId': !exists(json, 'icon_id') ? undefined : json['icon_id'],
        'name': !exists(json, 'name') ? undefined : json['name'],
        'anchorable': !exists(json, 'anchorable') ? undefined : json['anchorable'],
        'anchored': !exists(json, 'anchored') ? undefined : json['anchored'],
        'fittableNonSingleton': !exists(json, 'fittable_non_singleton') ? undefined : json['fittable_non_singleton'],
        'published': !exists(json, 'published') ? undefined : json['published'],
        'useBasePrice': !exists(json, 'use_base_price') ? undefined : json['use_base_price'],
        'typeIds': !exists(json, 'type_ids') ? undefined : json['type_ids'],
    };
}

export function InventoryGroupToJSON(value?: InventoryGroup | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'group_id': value.groupId,
        'category_id': value.categoryId,
        'icon_id': value.iconId,
        'name': value.name,
        'anchorable': value.anchorable,
        'anchored': value.anchored,
        'fittable_non_singleton': value.fittableNonSingleton,
        'published': value.published,
        'use_base_price': value.useBasePrice,
        'type_ids': value.typeIds,
    };
}
