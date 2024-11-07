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
import type { GetUniverseConstellationsConstellationIdPosition } from './GetUniverseConstellationsConstellationIdPosition';
import {
    GetUniverseConstellationsConstellationIdPositionFromJSON,
    GetUniverseConstellationsConstellationIdPositionFromJSONTyped,
    GetUniverseConstellationsConstellationIdPositionToJSON,
} from './GetUniverseConstellationsConstellationIdPosition';

/**
 * 200 ok object
 * @export
 * @interface GetUniverseConstellationsConstellationIdOk
 */
export interface GetUniverseConstellationsConstellationIdOk {
    /**
     * constellation_id integer
     * @type {number}
     * @memberof GetUniverseConstellationsConstellationIdOk
     */
    constellationId: number;
    /**
     * name string
     * @type {string}
     * @memberof GetUniverseConstellationsConstellationIdOk
     */
    name: string;
    /**
     * 
     * @type {GetUniverseConstellationsConstellationIdPosition}
     * @memberof GetUniverseConstellationsConstellationIdOk
     */
    position: GetUniverseConstellationsConstellationIdPosition;
    /**
     * The region this constellation is in
     * @type {number}
     * @memberof GetUniverseConstellationsConstellationIdOk
     */
    regionId: number;
    /**
     * systems array
     * @type {Array<number>}
     * @memberof GetUniverseConstellationsConstellationIdOk
     */
    systems: Array<number>;
}

/**
 * Check if a given object implements the GetUniverseConstellationsConstellationIdOk interface.
 */
export function instanceOfGetUniverseConstellationsConstellationIdOk(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "constellationId" in value;
    isInstance = isInstance && "name" in value;
    isInstance = isInstance && "position" in value;
    isInstance = isInstance && "regionId" in value;
    isInstance = isInstance && "systems" in value;

    return isInstance;
}

export function GetUniverseConstellationsConstellationIdOkFromJSON(json: any): GetUniverseConstellationsConstellationIdOk {
    return GetUniverseConstellationsConstellationIdOkFromJSONTyped(json, false);
}

export function GetUniverseConstellationsConstellationIdOkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetUniverseConstellationsConstellationIdOk {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'constellationId': json['constellation_id'],
        'name': json['name'],
        'position': GetUniverseConstellationsConstellationIdPositionFromJSON(json['position']),
        'regionId': json['region_id'],
        'systems': json['systems'],
    };
}

export function GetUniverseConstellationsConstellationIdOkToJSON(value?: GetUniverseConstellationsConstellationIdOk | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'constellation_id': value.constellationId,
        'name': value.name,
        'position': GetUniverseConstellationsConstellationIdPositionToJSON(value.position),
        'region_id': value.regionId,
        'systems': value.systems,
    };
}

