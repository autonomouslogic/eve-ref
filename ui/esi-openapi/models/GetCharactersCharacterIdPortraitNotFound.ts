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
 * No image server for this datasource
 * @export
 * @interface GetCharactersCharacterIdPortraitNotFound
 */
export interface GetCharactersCharacterIdPortraitNotFound {
    /**
     * error message
     * @type {string}
     * @memberof GetCharactersCharacterIdPortraitNotFound
     */
    error?: string;
}

/**
 * Check if a given object implements the GetCharactersCharacterIdPortraitNotFound interface.
 */
export function instanceOfGetCharactersCharacterIdPortraitNotFound(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetCharactersCharacterIdPortraitNotFoundFromJSON(json: any): GetCharactersCharacterIdPortraitNotFound {
    return GetCharactersCharacterIdPortraitNotFoundFromJSONTyped(json, false);
}

export function GetCharactersCharacterIdPortraitNotFoundFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCharactersCharacterIdPortraitNotFound {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'error': !exists(json, 'error') ? undefined : json['error'],
    };
}

export function GetCharactersCharacterIdPortraitNotFoundToJSON(value?: GetCharactersCharacterIdPortraitNotFound | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'error': value.error,
    };
}

