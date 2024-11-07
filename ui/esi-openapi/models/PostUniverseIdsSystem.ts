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
 * system object
 * @export
 * @interface PostUniverseIdsSystem
 */
export interface PostUniverseIdsSystem {
    /**
     * id integer
     * @type {number}
     * @memberof PostUniverseIdsSystem
     */
    id?: number;
    /**
     * name string
     * @type {string}
     * @memberof PostUniverseIdsSystem
     */
    name?: string;
}

/**
 * Check if a given object implements the PostUniverseIdsSystem interface.
 */
export function instanceOfPostUniverseIdsSystem(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function PostUniverseIdsSystemFromJSON(json: any): PostUniverseIdsSystem {
    return PostUniverseIdsSystemFromJSONTyped(json, false);
}

export function PostUniverseIdsSystemFromJSONTyped(json: any, ignoreDiscriminator: boolean): PostUniverseIdsSystem {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'id': !exists(json, 'id') ? undefined : json['id'],
        'name': !exists(json, 'name') ? undefined : json['name'],
    };
}

export function PostUniverseIdsSystemToJSON(value?: PostUniverseIdsSystem | null): any {
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

