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
 * @interface GetLoyaltyStoresCorporationIdOffersNotFound
 */
export interface GetLoyaltyStoresCorporationIdOffersNotFound {
    /**
     * Not found message
     * @type {string}
     * @memberof GetLoyaltyStoresCorporationIdOffersNotFound
     */
    error?: string;
}

/**
 * Check if a given object implements the GetLoyaltyStoresCorporationIdOffersNotFound interface.
 */
export function instanceOfGetLoyaltyStoresCorporationIdOffersNotFound(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetLoyaltyStoresCorporationIdOffersNotFoundFromJSON(json: any): GetLoyaltyStoresCorporationIdOffersNotFound {
    return GetLoyaltyStoresCorporationIdOffersNotFoundFromJSONTyped(json, false);
}

export function GetLoyaltyStoresCorporationIdOffersNotFoundFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetLoyaltyStoresCorporationIdOffersNotFound {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'error': !exists(json, 'error') ? undefined : json['error'],
    };
}

export function GetLoyaltyStoresCorporationIdOffersNotFoundToJSON(value?: GetLoyaltyStoresCorporationIdOffersNotFound | null): any {
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

