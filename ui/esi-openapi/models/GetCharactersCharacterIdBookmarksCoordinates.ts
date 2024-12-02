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
 * Optional object that is returned if a bookmark was made on a planet or a random location in space.
 * @export
 * @interface GetCharactersCharacterIdBookmarksCoordinates
 */
export interface GetCharactersCharacterIdBookmarksCoordinates {
    /**
     * x number
     * @type {number}
     * @memberof GetCharactersCharacterIdBookmarksCoordinates
     */
    x: number;
    /**
     * y number
     * @type {number}
     * @memberof GetCharactersCharacterIdBookmarksCoordinates
     */
    y: number;
    /**
     * z number
     * @type {number}
     * @memberof GetCharactersCharacterIdBookmarksCoordinates
     */
    z: number;
}

/**
 * Check if a given object implements the GetCharactersCharacterIdBookmarksCoordinates interface.
 */
export function instanceOfGetCharactersCharacterIdBookmarksCoordinates(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "x" in value;
    isInstance = isInstance && "y" in value;
    isInstance = isInstance && "z" in value;

    return isInstance;
}

export function GetCharactersCharacterIdBookmarksCoordinatesFromJSON(json: any): GetCharactersCharacterIdBookmarksCoordinates {
    return GetCharactersCharacterIdBookmarksCoordinatesFromJSONTyped(json, false);
}

export function GetCharactersCharacterIdBookmarksCoordinatesFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCharactersCharacterIdBookmarksCoordinates {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'x': json['x'],
        'y': json['y'],
        'z': json['z'],
    };
}

export function GetCharactersCharacterIdBookmarksCoordinatesToJSON(value?: GetCharactersCharacterIdBookmarksCoordinates | null): any {
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
