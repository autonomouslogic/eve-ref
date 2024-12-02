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
 * Forbidden
 * @export
 * @interface GetContractsPublicBidsContractIdForbidden
 */
export interface GetContractsPublicBidsContractIdForbidden {
    /**
     * Forbidden message
     * @type {string}
     * @memberof GetContractsPublicBidsContractIdForbidden
     */
    error?: string;
}

/**
 * Check if a given object implements the GetContractsPublicBidsContractIdForbidden interface.
 */
export function instanceOfGetContractsPublicBidsContractIdForbidden(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetContractsPublicBidsContractIdForbiddenFromJSON(json: any): GetContractsPublicBidsContractIdForbidden {
    return GetContractsPublicBidsContractIdForbiddenFromJSONTyped(json, false);
}

export function GetContractsPublicBidsContractIdForbiddenFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetContractsPublicBidsContractIdForbidden {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'error': !exists(json, 'error') ? undefined : json['error'],
    };
}

export function GetContractsPublicBidsContractIdForbiddenToJSON(value?: GetContractsPublicBidsContractIdForbidden | null): any {
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
