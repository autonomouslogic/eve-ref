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
 * @interface GetUniverseSystemsSystemIdPosition
 */
export interface GetUniverseSystemsSystemIdPosition {
    /**
     * x number
     * @type {number}
     * @memberof GetUniverseSystemsSystemIdPosition
     */
    x: number;
    /**
     * y number
     * @type {number}
     * @memberof GetUniverseSystemsSystemIdPosition
     */
    y: number;
    /**
     * z number
     * @type {number}
     * @memberof GetUniverseSystemsSystemIdPosition
     */
    z: number;
}

/**
 * Check if a given object implements the GetUniverseSystemsSystemIdPosition interface.
 */
export function instanceOfGetUniverseSystemsSystemIdPosition(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "x" in value;
    isInstance = isInstance && "y" in value;
    isInstance = isInstance && "z" in value;

    return isInstance;
}

export function GetUniverseSystemsSystemIdPositionFromJSON(json: any): GetUniverseSystemsSystemIdPosition {
    return GetUniverseSystemsSystemIdPositionFromJSONTyped(json, false);
}

export function GetUniverseSystemsSystemIdPositionFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetUniverseSystemsSystemIdPosition {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'x': json['x'],
        'y': json['y'],
        'z': json['z'],
    };
}

export function GetUniverseSystemsSystemIdPositionToJSON(value?: GetUniverseSystemsSystemIdPosition | null): any {
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

