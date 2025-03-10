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
 * Unprocessable entity
 * @export
 * @interface GetKillmailsKillmailIdKillmailHashUnprocessableEntity
 */
export interface GetKillmailsKillmailIdKillmailHashUnprocessableEntity {
    /**
     * Unprocessable entity message
     * @type {string}
     * @memberof GetKillmailsKillmailIdKillmailHashUnprocessableEntity
     */
    error?: string;
}

/**
 * Check if a given object implements the GetKillmailsKillmailIdKillmailHashUnprocessableEntity interface.
 */
export function instanceOfGetKillmailsKillmailIdKillmailHashUnprocessableEntity(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetKillmailsKillmailIdKillmailHashUnprocessableEntityFromJSON(json: any): GetKillmailsKillmailIdKillmailHashUnprocessableEntity {
    return GetKillmailsKillmailIdKillmailHashUnprocessableEntityFromJSONTyped(json, false);
}

export function GetKillmailsKillmailIdKillmailHashUnprocessableEntityFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetKillmailsKillmailIdKillmailHashUnprocessableEntity {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'error': !exists(json, 'error') ? undefined : json['error'],
    };
}

export function GetKillmailsKillmailIdKillmailHashUnprocessableEntityToJSON(value?: GetKillmailsKillmailIdKillmailHashUnprocessableEntity | null): any {
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

