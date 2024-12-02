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
 * @interface GetUniverseSystemsSystemIdNotFound
 */
export interface GetUniverseSystemsSystemIdNotFound {
    /**
     * Not found message
     * @type {string}
     * @memberof GetUniverseSystemsSystemIdNotFound
     */
    error?: string;
}

/**
 * Check if a given object implements the GetUniverseSystemsSystemIdNotFound interface.
 */
export function instanceOfGetUniverseSystemsSystemIdNotFound(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetUniverseSystemsSystemIdNotFoundFromJSON(json: any): GetUniverseSystemsSystemIdNotFound {
    return GetUniverseSystemsSystemIdNotFoundFromJSONTyped(json, false);
}

export function GetUniverseSystemsSystemIdNotFoundFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetUniverseSystemsSystemIdNotFound {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'error': !exists(json, 'error') ? undefined : json['error'],
    };
}

export function GetUniverseSystemsSystemIdNotFoundToJSON(value?: GetUniverseSystemsSystemIdNotFound | null): any {
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
