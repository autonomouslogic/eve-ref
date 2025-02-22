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
 * @interface GetUniverseStargatesStargateIdNotFound
 */
export interface GetUniverseStargatesStargateIdNotFound {
    /**
     * Not found message
     * @type {string}
     * @memberof GetUniverseStargatesStargateIdNotFound
     */
    error?: string;
}

/**
 * Check if a given object implements the GetUniverseStargatesStargateIdNotFound interface.
 */
export function instanceOfGetUniverseStargatesStargateIdNotFound(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetUniverseStargatesStargateIdNotFoundFromJSON(json: any): GetUniverseStargatesStargateIdNotFound {
    return GetUniverseStargatesStargateIdNotFoundFromJSONTyped(json, false);
}

export function GetUniverseStargatesStargateIdNotFoundFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetUniverseStargatesStargateIdNotFound {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'error': !exists(json, 'error') ? undefined : json['error'],
    };
}

export function GetUniverseStargatesStargateIdNotFoundToJSON(value?: GetUniverseStargatesStargateIdNotFound | null): any {
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

