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
 * @interface GetCorporationsCorporationIdContractsContractIdItemsError520
 */
export interface GetCorporationsCorporationIdContractsContractIdItemsError520 {
    /**
     * Error 520 message
     * @type {string}
     * @memberof GetCorporationsCorporationIdContractsContractIdItemsError520
     */
    error?: string;
}

/**
 * Check if a given object implements the GetCorporationsCorporationIdContractsContractIdItemsError520 interface.
 */
export function instanceOfGetCorporationsCorporationIdContractsContractIdItemsError520(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetCorporationsCorporationIdContractsContractIdItemsError520FromJSON(json: any): GetCorporationsCorporationIdContractsContractIdItemsError520 {
    return GetCorporationsCorporationIdContractsContractIdItemsError520FromJSONTyped(json, false);
}

export function GetCorporationsCorporationIdContractsContractIdItemsError520FromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCorporationsCorporationIdContractsContractIdItemsError520 {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'error': !exists(json, 'error') ? undefined : json['error'],
    };
}

export function GetCorporationsCorporationIdContractsContractIdItemsError520ToJSON(value?: GetCorporationsCorporationIdContractsContractIdItemsError520 | null): any {
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

