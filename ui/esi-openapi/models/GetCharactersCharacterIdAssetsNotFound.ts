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
 * Requested page does not exist
 * @export
 * @interface GetCharactersCharacterIdAssetsNotFound
 */
export interface GetCharactersCharacterIdAssetsNotFound {
    /**
     * error message
     * @type {string}
     * @memberof GetCharactersCharacterIdAssetsNotFound
     */
    error?: string;
}

/**
 * Check if a given object implements the GetCharactersCharacterIdAssetsNotFound interface.
 */
export function instanceOfGetCharactersCharacterIdAssetsNotFound(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetCharactersCharacterIdAssetsNotFoundFromJSON(json: any): GetCharactersCharacterIdAssetsNotFound {
    return GetCharactersCharacterIdAssetsNotFoundFromJSONTyped(json, false);
}

export function GetCharactersCharacterIdAssetsNotFoundFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCharactersCharacterIdAssetsNotFound {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'error': !exists(json, 'error') ? undefined : json['error'],
    };
}

export function GetCharactersCharacterIdAssetsNotFoundToJSON(value?: GetCharactersCharacterIdAssetsNotFound | null): any {
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

