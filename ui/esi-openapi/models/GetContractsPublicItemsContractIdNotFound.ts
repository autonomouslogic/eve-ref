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
 * @interface GetContractsPublicItemsContractIdNotFound
 */
export interface GetContractsPublicItemsContractIdNotFound {
    /**
     * Not found message
     * @type {string}
     * @memberof GetContractsPublicItemsContractIdNotFound
     */
    error?: string;
}

/**
 * Check if a given object implements the GetContractsPublicItemsContractIdNotFound interface.
 */
export function instanceOfGetContractsPublicItemsContractIdNotFound(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetContractsPublicItemsContractIdNotFoundFromJSON(json: any): GetContractsPublicItemsContractIdNotFound {
    return GetContractsPublicItemsContractIdNotFoundFromJSONTyped(json, false);
}

export function GetContractsPublicItemsContractIdNotFoundFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetContractsPublicItemsContractIdNotFound {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'error': !exists(json, 'error') ? undefined : json['error'],
    };
}

export function GetContractsPublicItemsContractIdNotFoundToJSON(value?: GetContractsPublicItemsContractIdNotFound | null): any {
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

