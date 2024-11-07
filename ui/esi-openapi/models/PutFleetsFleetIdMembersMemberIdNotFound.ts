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
 * @interface PutFleetsFleetIdMembersMemberIdNotFound
 */
export interface PutFleetsFleetIdMembersMemberIdNotFound {
    /**
     * Not found message
     * @type {string}
     * @memberof PutFleetsFleetIdMembersMemberIdNotFound
     */
    error?: string;
}

/**
 * Check if a given object implements the PutFleetsFleetIdMembersMemberIdNotFound interface.
 */
export function instanceOfPutFleetsFleetIdMembersMemberIdNotFound(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function PutFleetsFleetIdMembersMemberIdNotFoundFromJSON(json: any): PutFleetsFleetIdMembersMemberIdNotFound {
    return PutFleetsFleetIdMembersMemberIdNotFoundFromJSONTyped(json, false);
}

export function PutFleetsFleetIdMembersMemberIdNotFoundFromJSONTyped(json: any, ignoreDiscriminator: boolean): PutFleetsFleetIdMembersMemberIdNotFound {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'error': !exists(json, 'error') ? undefined : json['error'],
    };
}

export function PutFleetsFleetIdMembersMemberIdNotFoundToJSON(value?: PutFleetsFleetIdMembersMemberIdNotFound | null): any {
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

