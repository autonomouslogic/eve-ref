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
import type { GetFleetsFleetIdWingsSquad } from './GetFleetsFleetIdWingsSquad';
import {
    GetFleetsFleetIdWingsSquadFromJSON,
    GetFleetsFleetIdWingsSquadFromJSONTyped,
    GetFleetsFleetIdWingsSquadToJSON,
} from './GetFleetsFleetIdWingsSquad';

/**
 * 200 ok object
 * @export
 * @interface GetFleetsFleetIdWings200Ok
 */
export interface GetFleetsFleetIdWings200Ok {
    /**
     * id integer
     * @type {number}
     * @memberof GetFleetsFleetIdWings200Ok
     */
    id: number;
    /**
     * name string
     * @type {string}
     * @memberof GetFleetsFleetIdWings200Ok
     */
    name: string;
    /**
     * squads array
     * @type {Array<GetFleetsFleetIdWingsSquad>}
     * @memberof GetFleetsFleetIdWings200Ok
     */
    squads: Array<GetFleetsFleetIdWingsSquad>;
}

/**
 * Check if a given object implements the GetFleetsFleetIdWings200Ok interface.
 */
export function instanceOfGetFleetsFleetIdWings200Ok(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "id" in value;
    isInstance = isInstance && "name" in value;
    isInstance = isInstance && "squads" in value;

    return isInstance;
}

export function GetFleetsFleetIdWings200OkFromJSON(json: any): GetFleetsFleetIdWings200Ok {
    return GetFleetsFleetIdWings200OkFromJSONTyped(json, false);
}

export function GetFleetsFleetIdWings200OkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetFleetsFleetIdWings200Ok {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'id': json['id'],
        'name': json['name'],
        'squads': ((json['squads'] as Array<any>).map(GetFleetsFleetIdWingsSquadFromJSON)),
    };
}

export function GetFleetsFleetIdWings200OkToJSON(value?: GetFleetsFleetIdWings200Ok | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'id': value.id,
        'name': value.name,
        'squads': ((value.squads as Array<any>).map(GetFleetsFleetIdWingsSquadToJSON)),
    };
}

