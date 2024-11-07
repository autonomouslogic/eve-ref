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
 * home_location object
 * @export
 * @interface GetCharactersCharacterIdClonesHomeLocation
 */
export interface GetCharactersCharacterIdClonesHomeLocation {
    /**
     * location_id integer
     * @type {number}
     * @memberof GetCharactersCharacterIdClonesHomeLocation
     */
    locationId?: number;
    /**
     * location_type string
     * @type {string}
     * @memberof GetCharactersCharacterIdClonesHomeLocation
     */
    locationType?: GetCharactersCharacterIdClonesHomeLocationLocationTypeEnum;
}


/**
 * @export
 */
export const GetCharactersCharacterIdClonesHomeLocationLocationTypeEnum = {
    Station: 'station',
    Structure: 'structure'
} as const;
export type GetCharactersCharacterIdClonesHomeLocationLocationTypeEnum = typeof GetCharactersCharacterIdClonesHomeLocationLocationTypeEnum[keyof typeof GetCharactersCharacterIdClonesHomeLocationLocationTypeEnum];


/**
 * Check if a given object implements the GetCharactersCharacterIdClonesHomeLocation interface.
 */
export function instanceOfGetCharactersCharacterIdClonesHomeLocation(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetCharactersCharacterIdClonesHomeLocationFromJSON(json: any): GetCharactersCharacterIdClonesHomeLocation {
    return GetCharactersCharacterIdClonesHomeLocationFromJSONTyped(json, false);
}

export function GetCharactersCharacterIdClonesHomeLocationFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCharactersCharacterIdClonesHomeLocation {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'locationId': !exists(json, 'location_id') ? undefined : json['location_id'],
        'locationType': !exists(json, 'location_type') ? undefined : json['location_type'],
    };
}

export function GetCharactersCharacterIdClonesHomeLocationToJSON(value?: GetCharactersCharacterIdClonesHomeLocation | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'location_id': value.locationId,
        'location_type': value.locationType,
    };
}

