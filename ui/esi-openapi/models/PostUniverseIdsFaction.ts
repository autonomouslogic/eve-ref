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
 * faction object
 * @export
 * @interface PostUniverseIdsFaction
 */
export interface PostUniverseIdsFaction {
    /**
     * id integer
     * @type {number}
     * @memberof PostUniverseIdsFaction
     */
    id?: number;
    /**
     * name string
     * @type {string}
     * @memberof PostUniverseIdsFaction
     */
    name?: string;
}

/**
 * Check if a given object implements the PostUniverseIdsFaction interface.
 */
export function instanceOfPostUniverseIdsFaction(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function PostUniverseIdsFactionFromJSON(json: any): PostUniverseIdsFaction {
    return PostUniverseIdsFactionFromJSONTyped(json, false);
}

export function PostUniverseIdsFactionFromJSONTyped(json: any, ignoreDiscriminator: boolean): PostUniverseIdsFaction {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'id': !exists(json, 'id') ? undefined : json['id'],
        'name': !exists(json, 'name') ? undefined : json['name'],
    };
}

export function PostUniverseIdsFactionToJSON(value?: PostUniverseIdsFaction | null): any {
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

