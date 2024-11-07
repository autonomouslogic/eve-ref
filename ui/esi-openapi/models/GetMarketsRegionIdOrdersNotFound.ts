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
/**
 * Not found
 * @export
 * @interface GetMarketsRegionIdOrdersNotFound
 */
export interface GetMarketsRegionIdOrdersNotFound {
    /**
     * Not found message
     * @type {string}
     * @memberof GetMarketsRegionIdOrdersNotFound
     */
    error?: string;
}

/**
 * Check if a given object implements the GetMarketsRegionIdOrdersNotFound interface.
 */
export function instanceOfGetMarketsRegionIdOrdersNotFound(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetMarketsRegionIdOrdersNotFoundFromJSON(json: any): GetMarketsRegionIdOrdersNotFound {
    return GetMarketsRegionIdOrdersNotFoundFromJSONTyped(json, false);
}

export function GetMarketsRegionIdOrdersNotFoundFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetMarketsRegionIdOrdersNotFound {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'error': !exists(json, 'error') ? undefined : json['error'],
    };
}

export function GetMarketsRegionIdOrdersNotFoundToJSON(value?: GetMarketsRegionIdOrdersNotFound | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'error': value.error,
    };
}

