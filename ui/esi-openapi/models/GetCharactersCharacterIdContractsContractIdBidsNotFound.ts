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
 * @interface GetCharactersCharacterIdContractsContractIdBidsNotFound
 */
export interface GetCharactersCharacterIdContractsContractIdBidsNotFound {
    /**
     * Not found message
     * @type {string}
     * @memberof GetCharactersCharacterIdContractsContractIdBidsNotFound
     */
    error?: string;
}

/**
 * Check if a given object implements the GetCharactersCharacterIdContractsContractIdBidsNotFound interface.
 */
export function instanceOfGetCharactersCharacterIdContractsContractIdBidsNotFound(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetCharactersCharacterIdContractsContractIdBidsNotFoundFromJSON(json: any): GetCharactersCharacterIdContractsContractIdBidsNotFound {
    return GetCharactersCharacterIdContractsContractIdBidsNotFoundFromJSONTyped(json, false);
}

export function GetCharactersCharacterIdContractsContractIdBidsNotFoundFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCharactersCharacterIdContractsContractIdBidsNotFound {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'error': !exists(json, 'error') ? undefined : json['error'],
    };
}

export function GetCharactersCharacterIdContractsContractIdBidsNotFoundToJSON(value?: GetCharactersCharacterIdContractsContractIdBidsNotFound | null): any {
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

