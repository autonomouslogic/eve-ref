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
 * @interface GetCharactersCharacterIdCalendarEventIdAttendeesNotFound
 */
export interface GetCharactersCharacterIdCalendarEventIdAttendeesNotFound {
    /**
     * Not found message
     * @type {string}
     * @memberof GetCharactersCharacterIdCalendarEventIdAttendeesNotFound
     */
    error?: string;
}

/**
 * Check if a given object implements the GetCharactersCharacterIdCalendarEventIdAttendeesNotFound interface.
 */
export function instanceOfGetCharactersCharacterIdCalendarEventIdAttendeesNotFound(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetCharactersCharacterIdCalendarEventIdAttendeesNotFoundFromJSON(json: any): GetCharactersCharacterIdCalendarEventIdAttendeesNotFound {
    return GetCharactersCharacterIdCalendarEventIdAttendeesNotFoundFromJSONTyped(json, false);
}

export function GetCharactersCharacterIdCalendarEventIdAttendeesNotFoundFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCharactersCharacterIdCalendarEventIdAttendeesNotFound {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'error': !exists(json, 'error') ? undefined : json['error'],
    };
}

export function GetCharactersCharacterIdCalendarEventIdAttendeesNotFoundToJSON(value?: GetCharactersCharacterIdCalendarEventIdAttendeesNotFound | null): any {
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

