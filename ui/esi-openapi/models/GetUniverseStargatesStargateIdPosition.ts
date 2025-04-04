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
 * @interface GetUniverseStargatesStargateIdPosition
 */
export interface GetUniverseStargatesStargateIdPosition {
    /**
     * x number
     * @type {number}
     * @memberof GetUniverseStargatesStargateIdPosition
     */
    x: number;
    /**
     * y number
     * @type {number}
     * @memberof GetUniverseStargatesStargateIdPosition
     */
    y: number;
    /**
     * z number
     * @type {number}
     * @memberof GetUniverseStargatesStargateIdPosition
     */
    z: number;
}

/**
 * Check if a given object implements the GetUniverseStargatesStargateIdPosition interface.
 */
export function instanceOfGetUniverseStargatesStargateIdPosition(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "x" in value;
    isInstance = isInstance && "y" in value;
    isInstance = isInstance && "z" in value;

    return isInstance;
}

export function GetUniverseStargatesStargateIdPositionFromJSON(json: any): GetUniverseStargatesStargateIdPosition {
    return GetUniverseStargatesStargateIdPositionFromJSONTyped(json, false);
}

export function GetUniverseStargatesStargateIdPositionFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetUniverseStargatesStargateIdPosition {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'x': json['x'],
        'y': json['y'],
        'z': json['z'],
    };
}

export function GetUniverseStargatesStargateIdPositionToJSON(value?: GetUniverseStargatesStargateIdPosition | null): any {
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

