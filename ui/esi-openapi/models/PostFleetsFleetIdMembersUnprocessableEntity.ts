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
 * 422 unprocessable entity object
 * @export
 * @interface PostFleetsFleetIdMembersUnprocessableEntity
 */
export interface PostFleetsFleetIdMembersUnprocessableEntity {
    /**
     * error message
     * @type {string}
     * @memberof PostFleetsFleetIdMembersUnprocessableEntity
     */
    error?: string;
}

/**
 * Check if a given object implements the PostFleetsFleetIdMembersUnprocessableEntity interface.
 */
export function instanceOfPostFleetsFleetIdMembersUnprocessableEntity(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function PostFleetsFleetIdMembersUnprocessableEntityFromJSON(json: any): PostFleetsFleetIdMembersUnprocessableEntity {
    return PostFleetsFleetIdMembersUnprocessableEntityFromJSONTyped(json, false);
}

export function PostFleetsFleetIdMembersUnprocessableEntityFromJSONTyped(json: any, ignoreDiscriminator: boolean): PostFleetsFleetIdMembersUnprocessableEntity {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'error': !exists(json, 'error') ? undefined : json['error'],
    };
}

export function PostFleetsFleetIdMembersUnprocessableEntityToJSON(value?: PostFleetsFleetIdMembersUnprocessableEntity | null): any {
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

