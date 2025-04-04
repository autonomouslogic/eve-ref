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
 * @interface GetUniverseGroupsGroupIdNotFound
 */
export interface GetUniverseGroupsGroupIdNotFound {
    /**
     * Not found message
     * @type {string}
     * @memberof GetUniverseGroupsGroupIdNotFound
     */
    error?: string;
}

/**
 * Check if a given object implements the GetUniverseGroupsGroupIdNotFound interface.
 */
export function instanceOfGetUniverseGroupsGroupIdNotFound(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetUniverseGroupsGroupIdNotFoundFromJSON(json: any): GetUniverseGroupsGroupIdNotFound {
    return GetUniverseGroupsGroupIdNotFoundFromJSONTyped(json, false);
}

export function GetUniverseGroupsGroupIdNotFoundFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetUniverseGroupsGroupIdNotFound {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'error': !exists(json, 'error') ? undefined : json['error'],
    };
}

export function GetUniverseGroupsGroupIdNotFoundToJSON(value?: GetUniverseGroupsGroupIdNotFound | null): any {
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

