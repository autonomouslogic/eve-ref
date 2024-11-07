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
 * Error 520
 * @export
 * @interface GetMarketsRegionIdHistoryError520
 */
export interface GetMarketsRegionIdHistoryError520 {
    /**
     * Error 520 message
     * @type {string}
     * @memberof GetMarketsRegionIdHistoryError520
     */
    error?: string;
}

/**
 * Check if a given object implements the GetMarketsRegionIdHistoryError520 interface.
 */
export function instanceOfGetMarketsRegionIdHistoryError520(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetMarketsRegionIdHistoryError520FromJSON(json: any): GetMarketsRegionIdHistoryError520 {
    return GetMarketsRegionIdHistoryError520FromJSONTyped(json, false);
}

export function GetMarketsRegionIdHistoryError520FromJSONTyped(json: any, ignoreDiscriminator: boolean): GetMarketsRegionIdHistoryError520 {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'error': !exists(json, 'error') ? undefined : json['error'],
    };
}

export function GetMarketsRegionIdHistoryError520ToJSON(value?: GetMarketsRegionIdHistoryError520 | null): any {
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

