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
 * Not found
 * @export
 * @interface GetUniverseAsteroidBeltsAsteroidBeltIdNotFound
 */
export interface GetUniverseAsteroidBeltsAsteroidBeltIdNotFound {
    /**
     * Not found message
     * @type {string}
     * @memberof GetUniverseAsteroidBeltsAsteroidBeltIdNotFound
     */
    error?: string;
}

/**
 * Check if a given object implements the GetUniverseAsteroidBeltsAsteroidBeltIdNotFound interface.
 */
export function instanceOfGetUniverseAsteroidBeltsAsteroidBeltIdNotFound(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetUniverseAsteroidBeltsAsteroidBeltIdNotFoundFromJSON(json: any): GetUniverseAsteroidBeltsAsteroidBeltIdNotFound {
    return GetUniverseAsteroidBeltsAsteroidBeltIdNotFoundFromJSONTyped(json, false);
}

export function GetUniverseAsteroidBeltsAsteroidBeltIdNotFoundFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetUniverseAsteroidBeltsAsteroidBeltIdNotFound {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'error': !exists(json, 'error') ? undefined : json['error'],
    };
}

export function GetUniverseAsteroidBeltsAsteroidBeltIdNotFoundToJSON(value?: GetUniverseAsteroidBeltsAsteroidBeltIdNotFound | null): any {
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

