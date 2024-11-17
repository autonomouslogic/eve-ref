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
 * required_item object
 * @export
 * @interface GetLoyaltyStoresCorporationIdOffersRequiredItem
 */
export interface GetLoyaltyStoresCorporationIdOffersRequiredItem {
    /**
     * quantity integer
     * @type {number}
     * @memberof GetLoyaltyStoresCorporationIdOffersRequiredItem
     */
    quantity: number;
    /**
     * type_id integer
     * @type {number}
     * @memberof GetLoyaltyStoresCorporationIdOffersRequiredItem
     */
    typeId: number;
}

/**
 * Check if a given object implements the GetLoyaltyStoresCorporationIdOffersRequiredItem interface.
 */
export function instanceOfGetLoyaltyStoresCorporationIdOffersRequiredItem(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "quantity" in value;
    isInstance = isInstance && "typeId" in value;

    return isInstance;
}

export function GetLoyaltyStoresCorporationIdOffersRequiredItemFromJSON(json: any): GetLoyaltyStoresCorporationIdOffersRequiredItem {
    return GetLoyaltyStoresCorporationIdOffersRequiredItemFromJSONTyped(json, false);
}

export function GetLoyaltyStoresCorporationIdOffersRequiredItemFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetLoyaltyStoresCorporationIdOffersRequiredItem {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'quantity': json['quantity'],
        'typeId': json['type_id'],
    };
}

export function GetLoyaltyStoresCorporationIdOffersRequiredItemToJSON(value?: GetLoyaltyStoresCorporationIdOffersRequiredItem | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'quantity': value.quantity,
        'type_id': value.typeId,
    };
}

