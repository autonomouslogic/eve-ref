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
import type { GetCharactersCharacterIdMailMailIdRecipient } from './GetCharactersCharacterIdMailMailIdRecipient';
import {
    GetCharactersCharacterIdMailMailIdRecipientFromJSON,
    GetCharactersCharacterIdMailMailIdRecipientFromJSONTyped,
    GetCharactersCharacterIdMailMailIdRecipientToJSON,
} from './GetCharactersCharacterIdMailMailIdRecipient';

/**
 * 200 ok object
 * @export
 * @interface GetCharactersCharacterIdMailMailIdOk
 */
export interface GetCharactersCharacterIdMailMailIdOk {
    /**
     * Mail's body
     * @type {string}
     * @memberof GetCharactersCharacterIdMailMailIdOk
     */
    body?: string;
    /**
     * From whom the mail was sent
     * @type {number}
     * @memberof GetCharactersCharacterIdMailMailIdOk
     */
    from?: number;
    /**
     * Labels attached to the mail
     * @type {Array<number>}
     * @memberof GetCharactersCharacterIdMailMailIdOk
     */
    labels?: Array<number>;
    /**
     * Whether the mail is flagged as read
     * @type {boolean}
     * @memberof GetCharactersCharacterIdMailMailIdOk
     */
    read?: boolean;
    /**
     * Recipients of the mail
     * @type {Set<GetCharactersCharacterIdMailMailIdRecipient>}
     * @memberof GetCharactersCharacterIdMailMailIdOk
     */
    recipients?: Set<GetCharactersCharacterIdMailMailIdRecipient>;
    /**
     * Mail subject
     * @type {string}
     * @memberof GetCharactersCharacterIdMailMailIdOk
     */
    subject?: string;
    /**
     * When the mail was sent
     * @type {Date}
     * @memberof GetCharactersCharacterIdMailMailIdOk
     */
    timestamp?: Date;
}

/**
 * Check if a given object implements the GetCharactersCharacterIdMailMailIdOk interface.
 */
export function instanceOfGetCharactersCharacterIdMailMailIdOk(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetCharactersCharacterIdMailMailIdOkFromJSON(json: any): GetCharactersCharacterIdMailMailIdOk {
    return GetCharactersCharacterIdMailMailIdOkFromJSONTyped(json, false);
}

export function GetCharactersCharacterIdMailMailIdOkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCharactersCharacterIdMailMailIdOk {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'body': !exists(json, 'body') ? undefined : json['body'],
        'from': !exists(json, 'from') ? undefined : json['from'],
        'labels': !exists(json, 'labels') ? undefined : json['labels'],
        'read': !exists(json, 'read') ? undefined : json['read'],
        'recipients': !exists(json, 'recipients') ? undefined : (new Set((json['recipients'] as Array<any>).map(GetCharactersCharacterIdMailMailIdRecipientFromJSON))),
        'subject': !exists(json, 'subject') ? undefined : json['subject'],
        'timestamp': !exists(json, 'timestamp') ? undefined : (new Date(json['timestamp'])),
    };
}

export function GetCharactersCharacterIdMailMailIdOkToJSON(value?: GetCharactersCharacterIdMailMailIdOk | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'body': value.body,
        'from': value.from,
        'labels': value.labels,
        'read': value.read,
        'recipients': value.recipients === undefined ? undefined : (Array.from(value.recipients as Set<any>).map(GetCharactersCharacterIdMailMailIdRecipientToJSON)),
        'subject': value.subject,
        'timestamp': value.timestamp === undefined ? undefined : (value.timestamp.toISOString()),
    };
}
