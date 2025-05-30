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
 * fuel object
 * @export
 * @interface GetCorporationsCorporationIdStarbasesStarbaseIdFuel
 */
export interface GetCorporationsCorporationIdStarbasesStarbaseIdFuel {
    /**
     * quantity integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdStarbasesStarbaseIdFuel
     */
    quantity: number;
    /**
     * type_id integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdStarbasesStarbaseIdFuel
     */
    typeId: number;
}

/**
 * Check if a given object implements the GetCorporationsCorporationIdStarbasesStarbaseIdFuel interface.
 */
export function instanceOfGetCorporationsCorporationIdStarbasesStarbaseIdFuel(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "quantity" in value;
    isInstance = isInstance && "typeId" in value;

    return isInstance;
}

export function GetCorporationsCorporationIdStarbasesStarbaseIdFuelFromJSON(json: any): GetCorporationsCorporationIdStarbasesStarbaseIdFuel {
    return GetCorporationsCorporationIdStarbasesStarbaseIdFuelFromJSONTyped(json, false);
}

export function GetCorporationsCorporationIdStarbasesStarbaseIdFuelFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCorporationsCorporationIdStarbasesStarbaseIdFuel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'quantity': json['quantity'],
        'typeId': json['type_id'],
    };
}

export function GetCorporationsCorporationIdStarbasesStarbaseIdFuelToJSON(value?: GetCorporationsCorporationIdStarbasesStarbaseIdFuel | null): any {
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

