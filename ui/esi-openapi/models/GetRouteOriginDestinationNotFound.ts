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
 * @interface GetRouteOriginDestinationNotFound
 */
export interface GetRouteOriginDestinationNotFound {
    /**
     * Not found message
     * @type {string}
     * @memberof GetRouteOriginDestinationNotFound
     */
    error?: string;
}

/**
 * Check if a given object implements the GetRouteOriginDestinationNotFound interface.
 */
export function instanceOfGetRouteOriginDestinationNotFound(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetRouteOriginDestinationNotFoundFromJSON(json: any): GetRouteOriginDestinationNotFound {
    return GetRouteOriginDestinationNotFoundFromJSONTyped(json, false);
}

export function GetRouteOriginDestinationNotFoundFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetRouteOriginDestinationNotFound {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'error': !exists(json, 'error') ? undefined : json['error'],
    };
}

export function GetRouteOriginDestinationNotFoundToJSON(value?: GetRouteOriginDestinationNotFound | null): any {
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

