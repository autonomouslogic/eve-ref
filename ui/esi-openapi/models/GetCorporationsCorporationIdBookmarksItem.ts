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
 * Optional object that is returned if a bookmark was made on a particular item.
 * @export
 * @interface GetCorporationsCorporationIdBookmarksItem
 */
export interface GetCorporationsCorporationIdBookmarksItem {
    /**
     * item_id integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdBookmarksItem
     */
    itemId: number;
    /**
     * type_id integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdBookmarksItem
     */
    typeId: number;
}

/**
 * Check if a given object implements the GetCorporationsCorporationIdBookmarksItem interface.
 */
export function instanceOfGetCorporationsCorporationIdBookmarksItem(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "itemId" in value;
    isInstance = isInstance && "typeId" in value;

    return isInstance;
}

export function GetCorporationsCorporationIdBookmarksItemFromJSON(json: any): GetCorporationsCorporationIdBookmarksItem {
    return GetCorporationsCorporationIdBookmarksItemFromJSONTyped(json, false);
}

export function GetCorporationsCorporationIdBookmarksItemFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCorporationsCorporationIdBookmarksItem {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'itemId': json['item_id'],
        'typeId': json['type_id'],
    };
}

export function GetCorporationsCorporationIdBookmarksItemToJSON(value?: GetCorporationsCorporationIdBookmarksItem | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'item_id': value.itemId,
        'type_id': value.typeId,
    };
}

