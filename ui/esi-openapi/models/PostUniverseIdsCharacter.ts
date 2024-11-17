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
 * character object
 * @export
 * @interface PostUniverseIdsCharacter
 */
export interface PostUniverseIdsCharacter {
    /**
     * id integer
     * @type {number}
     * @memberof PostUniverseIdsCharacter
     */
    id?: number;
    /**
     * name string
     * @type {string}
     * @memberof PostUniverseIdsCharacter
     */
    name?: string;
}

/**
 * Check if a given object implements the PostUniverseIdsCharacter interface.
 */
export function instanceOfPostUniverseIdsCharacter(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function PostUniverseIdsCharacterFromJSON(json: any): PostUniverseIdsCharacter {
    return PostUniverseIdsCharacterFromJSONTyped(json, false);
}

export function PostUniverseIdsCharacterFromJSONTyped(json: any, ignoreDiscriminator: boolean): PostUniverseIdsCharacter {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'id': !exists(json, 'id') ? undefined : json['id'],
        'name': !exists(json, 'name') ? undefined : json['name'],
    };
}

export function PostUniverseIdsCharacterToJSON(value?: PostUniverseIdsCharacter | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'id': value.id,
        'name': value.name,
    };
}

