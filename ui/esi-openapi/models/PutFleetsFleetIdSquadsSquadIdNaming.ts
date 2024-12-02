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
 * naming object
 * @export
 * @interface PutFleetsFleetIdSquadsSquadIdNaming
 */
export interface PutFleetsFleetIdSquadsSquadIdNaming {
    /**
     * name string
     * @type {string}
     * @memberof PutFleetsFleetIdSquadsSquadIdNaming
     */
    name: string;
}

/**
 * Check if a given object implements the PutFleetsFleetIdSquadsSquadIdNaming interface.
 */
export function instanceOfPutFleetsFleetIdSquadsSquadIdNaming(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "name" in value;

    return isInstance;
}

export function PutFleetsFleetIdSquadsSquadIdNamingFromJSON(json: any): PutFleetsFleetIdSquadsSquadIdNaming {
    return PutFleetsFleetIdSquadsSquadIdNamingFromJSONTyped(json, false);
}

export function PutFleetsFleetIdSquadsSquadIdNamingFromJSONTyped(json: any, ignoreDiscriminator: boolean): PutFleetsFleetIdSquadsSquadIdNaming {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'name': json['name'],
    };
}

export function PutFleetsFleetIdSquadsSquadIdNamingToJSON(value?: PutFleetsFleetIdSquadsSquadIdNaming | null): any {
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
