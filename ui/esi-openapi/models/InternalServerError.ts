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
 * Internal server error model
 * @export
 * @interface InternalServerError
 */
export interface InternalServerError {
    /**
     * Internal server error message
     * @type {string}
     * @memberof InternalServerError
     */
    error: string;
}

/**
 * Check if a given object implements the InternalServerError interface.
 */
export function instanceOfInternalServerError(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "error" in value;

    return isInstance;
}

export function InternalServerErrorFromJSON(json: any): InternalServerError {
    return InternalServerErrorFromJSONTyped(json, false);
}

export function InternalServerErrorFromJSONTyped(json: any, ignoreDiscriminator: boolean): InternalServerError {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'error': json['error'],
    };
}

export function InternalServerErrorToJSON(value?: InternalServerError | null): any {
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

