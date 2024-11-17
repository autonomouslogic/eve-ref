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
 * Not found
 * @export
 * @interface GetUniverseRegionsRegionIdNotFound
 */
export interface GetUniverseRegionsRegionIdNotFound {
    /**
     * Not found message
     * @type {string}
     * @memberof GetUniverseRegionsRegionIdNotFound
     */
    error?: string;
}

/**
 * Check if a given object implements the GetUniverseRegionsRegionIdNotFound interface.
 */
export function instanceOfGetUniverseRegionsRegionIdNotFound(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetUniverseRegionsRegionIdNotFoundFromJSON(json: any): GetUniverseRegionsRegionIdNotFound {
    return GetUniverseRegionsRegionIdNotFoundFromJSONTyped(json, false);
}

export function GetUniverseRegionsRegionIdNotFoundFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetUniverseRegionsRegionIdNotFound {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'error': !exists(json, 'error') ? undefined : json['error'],
    };
}

export function GetUniverseRegionsRegionIdNotFoundToJSON(value?: GetUniverseRegionsRegionIdNotFound | null): any {
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

