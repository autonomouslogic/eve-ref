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
 * 200 ok object
 * @export
 * @interface PostCorporationsCorporationIdAssetsNames200Ok
 */
export interface PostCorporationsCorporationIdAssetsNames200Ok {
    /**
     * item_id integer
     * @type {number}
     * @memberof PostCorporationsCorporationIdAssetsNames200Ok
     */
    itemId: number;
    /**
     * name string
     * @type {string}
     * @memberof PostCorporationsCorporationIdAssetsNames200Ok
     */
    name: string;
}

/**
 * Check if a given object implements the PostCorporationsCorporationIdAssetsNames200Ok interface.
 */
export function instanceOfPostCorporationsCorporationIdAssetsNames200Ok(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "itemId" in value;
    isInstance = isInstance && "name" in value;

    return isInstance;
}

export function PostCorporationsCorporationIdAssetsNames200OkFromJSON(json: any): PostCorporationsCorporationIdAssetsNames200Ok {
    return PostCorporationsCorporationIdAssetsNames200OkFromJSONTyped(json, false);
}

export function PostCorporationsCorporationIdAssetsNames200OkFromJSONTyped(json: any, ignoreDiscriminator: boolean): PostCorporationsCorporationIdAssetsNames200Ok {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'itemId': json['item_id'],
        'name': json['name'],
    };
}

export function PostCorporationsCorporationIdAssetsNames200OkToJSON(value?: PostCorporationsCorporationIdAssetsNames200Ok | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'item_id': value.itemId,
        'name': value.name,
    };
}

