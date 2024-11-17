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
 * @interface GetContractsPublicRegionIdNotFound
 */
export interface GetContractsPublicRegionIdNotFound {
    /**
     * Not found message
     * @type {string}
     * @memberof GetContractsPublicRegionIdNotFound
     */
    error?: string;
}

/**
 * Check if a given object implements the GetContractsPublicRegionIdNotFound interface.
 */
export function instanceOfGetContractsPublicRegionIdNotFound(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetContractsPublicRegionIdNotFoundFromJSON(json: any): GetContractsPublicRegionIdNotFound {
    return GetContractsPublicRegionIdNotFoundFromJSONTyped(json, false);
}

export function GetContractsPublicRegionIdNotFoundFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetContractsPublicRegionIdNotFound {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'error': !exists(json, 'error') ? undefined : json['error'],
    };
}

export function GetContractsPublicRegionIdNotFoundToJSON(value?: GetContractsPublicRegionIdNotFound | null): any {
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

