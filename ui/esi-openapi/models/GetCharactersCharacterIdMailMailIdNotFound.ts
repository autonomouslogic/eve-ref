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
 * @interface GetCharactersCharacterIdMailMailIdNotFound
 */
export interface GetCharactersCharacterIdMailMailIdNotFound {
    /**
     * Not found message
     * @type {string}
     * @memberof GetCharactersCharacterIdMailMailIdNotFound
     */
    error?: string;
}

/**
 * Check if a given object implements the GetCharactersCharacterIdMailMailIdNotFound interface.
 */
export function instanceOfGetCharactersCharacterIdMailMailIdNotFound(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetCharactersCharacterIdMailMailIdNotFoundFromJSON(json: any): GetCharactersCharacterIdMailMailIdNotFound {
    return GetCharactersCharacterIdMailMailIdNotFoundFromJSONTyped(json, false);
}

export function GetCharactersCharacterIdMailMailIdNotFoundFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCharactersCharacterIdMailMailIdNotFound {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'error': !exists(json, 'error') ? undefined : json['error'],
    };
}

export function GetCharactersCharacterIdMailMailIdNotFoundToJSON(value?: GetCharactersCharacterIdMailMailIdNotFound | null): any {
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

