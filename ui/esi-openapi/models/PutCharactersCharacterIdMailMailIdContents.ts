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
 * contents object
 * @export
 * @interface PutCharactersCharacterIdMailMailIdContents
 */
export interface PutCharactersCharacterIdMailMailIdContents {
    /**
     * Labels to assign to the mail. Pre-existing labels are unassigned.
     * @type {Array<number>}
     * @memberof PutCharactersCharacterIdMailMailIdContents
     */
    labels?: Array<number>;
    /**
     * Whether the mail is flagged as read
     * @type {boolean}
     * @memberof PutCharactersCharacterIdMailMailIdContents
     */
    read?: boolean;
}

/**
 * Check if a given object implements the PutCharactersCharacterIdMailMailIdContents interface.
 */
export function instanceOfPutCharactersCharacterIdMailMailIdContents(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function PutCharactersCharacterIdMailMailIdContentsFromJSON(json: any): PutCharactersCharacterIdMailMailIdContents {
    return PutCharactersCharacterIdMailMailIdContentsFromJSONTyped(json, false);
}

export function PutCharactersCharacterIdMailMailIdContentsFromJSONTyped(json: any, ignoreDiscriminator: boolean): PutCharactersCharacterIdMailMailIdContents {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'labels': !exists(json, 'labels') ? undefined : json['labels'],
        'read': !exists(json, 'read') ? undefined : json['read'],
    };
}

export function PutCharactersCharacterIdMailMailIdContentsToJSON(value?: PutCharactersCharacterIdMailMailIdContents | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'labels': value.labels,
        'read': value.read,
    };
}

