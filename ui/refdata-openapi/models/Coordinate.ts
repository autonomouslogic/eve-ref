/* tslint:disable */
/* eslint-disable */
/**
 * EVE Ref Reference Data for EVE Online
 * This spec should be considered unstable and subject to change at any time.
 *
 * The version of the OpenAPI document: dev
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { exists, mapValues } from '../runtime';
/**
 * 
 * @export
 * @interface Coordinate
 */
export interface Coordinate {
    /**
     * 
     * @type {number}
     * @memberof Coordinate
     */
    x?: number;
    /**
     * 
     * @type {number}
     * @memberof Coordinate
     */
    y?: number;
    /**
     * 
     * @type {number}
     * @memberof Coordinate
     */
    z?: number;
}

/**
 * Check if a given object implements the Coordinate interface.
 */
export function instanceOfCoordinate(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function CoordinateFromJSON(json: any): Coordinate {
    return CoordinateFromJSONTyped(json, false);
}

export function CoordinateFromJSONTyped(json: any, ignoreDiscriminator: boolean): Coordinate {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'x': !exists(json, 'x') ? undefined : json['x'],
        'y': !exists(json, 'y') ? undefined : json['y'],
        'z': !exists(json, 'z') ? undefined : json['z'],
    };
}

export function CoordinateToJSON(value?: Coordinate | null): any {
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
