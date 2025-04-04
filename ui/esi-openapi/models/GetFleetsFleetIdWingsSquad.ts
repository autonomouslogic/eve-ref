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
 * squad object
 * @export
 * @interface GetFleetsFleetIdWingsSquad
 */
export interface GetFleetsFleetIdWingsSquad {
    /**
     * id integer
     * @type {number}
     * @memberof GetFleetsFleetIdWingsSquad
     */
    id: number;
    /**
     * name string
     * @type {string}
     * @memberof GetFleetsFleetIdWingsSquad
     */
    name: string;
}

/**
 * Check if a given object implements the GetFleetsFleetIdWingsSquad interface.
 */
export function instanceOfGetFleetsFleetIdWingsSquad(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "id" in value;
    isInstance = isInstance && "name" in value;

    return isInstance;
}

export function GetFleetsFleetIdWingsSquadFromJSON(json: any): GetFleetsFleetIdWingsSquad {
    return GetFleetsFleetIdWingsSquadFromJSONTyped(json, false);
}

export function GetFleetsFleetIdWingsSquadFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetFleetsFleetIdWingsSquad {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'id': json['id'],
        'name': json['name'],
    };
}

export function GetFleetsFleetIdWingsSquadToJSON(value?: GetFleetsFleetIdWingsSquad | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'id': value.id,
        'name': value.name,
    };
}

