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
 * level object
 * @export
 * @interface GetInsurancePricesLevel
 */
export interface GetInsurancePricesLevel {
    /**
     * cost number
     * @type {number}
     * @memberof GetInsurancePricesLevel
     */
    cost: number;
    /**
     * Localized insurance level
     * @type {string}
     * @memberof GetInsurancePricesLevel
     */
    name: string;
    /**
     * payout number
     * @type {number}
     * @memberof GetInsurancePricesLevel
     */
    payout: number;
}

/**
 * Check if a given object implements the GetInsurancePricesLevel interface.
 */
export function instanceOfGetInsurancePricesLevel(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "cost" in value;
    isInstance = isInstance && "name" in value;
    isInstance = isInstance && "payout" in value;

    return isInstance;
}

export function GetInsurancePricesLevelFromJSON(json: any): GetInsurancePricesLevel {
    return GetInsurancePricesLevelFromJSONTyped(json, false);
}

export function GetInsurancePricesLevelFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetInsurancePricesLevel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'cost': json['cost'],
        'name': json['name'],
        'payout': json['payout'],
    };
}

export function GetInsurancePricesLevelToJSON(value?: GetInsurancePricesLevel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'cost': value.cost,
        'name': value.name,
        'payout': value.payout,
    };
}

