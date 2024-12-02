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
 * position object
 * @export
 * @interface GetUniverseStationsStationIdPosition
 */
export interface GetUniverseStationsStationIdPosition {
    /**
     * x number
     * @type {number}
     * @memberof GetUniverseStationsStationIdPosition
     */
    x: number;
    /**
     * y number
     * @type {number}
     * @memberof GetUniverseStationsStationIdPosition
     */
    y: number;
    /**
     * z number
     * @type {number}
     * @memberof GetUniverseStationsStationIdPosition
     */
    z: number;
}

/**
 * Check if a given object implements the GetUniverseStationsStationIdPosition interface.
 */
export function instanceOfGetUniverseStationsStationIdPosition(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "x" in value;
    isInstance = isInstance && "y" in value;
    isInstance = isInstance && "z" in value;

    return isInstance;
}

export function GetUniverseStationsStationIdPositionFromJSON(json: any): GetUniverseStationsStationIdPosition {
    return GetUniverseStationsStationIdPositionFromJSONTyped(json, false);
}

export function GetUniverseStationsStationIdPositionFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetUniverseStationsStationIdPosition {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'x': json['x'],
        'y': json['y'],
        'z': json['z'],
    };
}

export function GetUniverseStationsStationIdPositionToJSON(value?: GetUniverseStationsStationIdPosition | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'x': value.x,
        'y': value.y,
        'z': value.z,
    };
}
