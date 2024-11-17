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
 * naming object
 * @export
 * @interface PutFleetsFleetIdWingsWingIdNaming
 */
export interface PutFleetsFleetIdWingsWingIdNaming {
    /**
     * name string
     * @type {string}
     * @memberof PutFleetsFleetIdWingsWingIdNaming
     */
    name: string;
}

/**
 * Check if a given object implements the PutFleetsFleetIdWingsWingIdNaming interface.
 */
export function instanceOfPutFleetsFleetIdWingsWingIdNaming(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "name" in value;

    return isInstance;
}

export function PutFleetsFleetIdWingsWingIdNamingFromJSON(json: any): PutFleetsFleetIdWingsWingIdNaming {
    return PutFleetsFleetIdWingsWingIdNamingFromJSONTyped(json, false);
}

export function PutFleetsFleetIdWingsWingIdNamingFromJSONTyped(json: any, ignoreDiscriminator: boolean): PutFleetsFleetIdWingsWingIdNaming {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'name': json['name'],
    };
}

export function PutFleetsFleetIdWingsWingIdNamingToJSON(value?: PutFleetsFleetIdWingsWingIdNaming | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'name': value.name,
    };
}

