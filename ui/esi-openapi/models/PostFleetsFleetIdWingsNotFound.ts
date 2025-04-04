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
 * @interface PostFleetsFleetIdWingsNotFound
 */
export interface PostFleetsFleetIdWingsNotFound {
    /**
     * Not found message
     * @type {string}
     * @memberof PostFleetsFleetIdWingsNotFound
     */
    error?: string;
}

/**
 * Check if a given object implements the PostFleetsFleetIdWingsNotFound interface.
 */
export function instanceOfPostFleetsFleetIdWingsNotFound(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function PostFleetsFleetIdWingsNotFoundFromJSON(json: any): PostFleetsFleetIdWingsNotFound {
    return PostFleetsFleetIdWingsNotFoundFromJSONTyped(json, false);
}

export function PostFleetsFleetIdWingsNotFoundFromJSONTyped(json: any, ignoreDiscriminator: boolean): PostFleetsFleetIdWingsNotFound {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'error': !exists(json, 'error') ? undefined : json['error'],
    };
}

export function PostFleetsFleetIdWingsNotFoundToJSON(value?: PostFleetsFleetIdWingsNotFound | null): any {
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

