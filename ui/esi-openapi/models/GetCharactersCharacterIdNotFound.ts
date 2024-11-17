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
 * Not found
 * @export
 * @interface GetCharactersCharacterIdNotFound
 */
export interface GetCharactersCharacterIdNotFound {
    /**
     * Not found message
     * @type {string}
     * @memberof GetCharactersCharacterIdNotFound
     */
    error?: string;
}

/**
 * Check if a given object implements the GetCharactersCharacterIdNotFound interface.
 */
export function instanceOfGetCharactersCharacterIdNotFound(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetCharactersCharacterIdNotFoundFromJSON(json: any): GetCharactersCharacterIdNotFound {
    return GetCharactersCharacterIdNotFoundFromJSONTyped(json, false);
}

export function GetCharactersCharacterIdNotFoundFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCharactersCharacterIdNotFound {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'error': !exists(json, 'error') ? undefined : json['error'],
    };
}

export function GetCharactersCharacterIdNotFoundToJSON(value?: GetCharactersCharacterIdNotFound | null): any {
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

