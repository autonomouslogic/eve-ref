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
 * recipient object
 * @export
 * @interface PostCharactersCharacterIdMailRecipient
 */
export interface PostCharactersCharacterIdMailRecipient {
    /**
     * recipient_id integer
     * @type {number}
     * @memberof PostCharactersCharacterIdMailRecipient
     */
    recipientId: number;
    /**
     * recipient_type string
     * @type {string}
     * @memberof PostCharactersCharacterIdMailRecipient
     */
    recipientType: PostCharactersCharacterIdMailRecipientRecipientTypeEnum;
}


/**
 * @export
 */
export const PostCharactersCharacterIdMailRecipientRecipientTypeEnum = {
    Alliance: 'alliance',
    Character: 'character',
    Corporation: 'corporation',
    MailingList: 'mailing_list'
} as const;
export type PostCharactersCharacterIdMailRecipientRecipientTypeEnum = typeof PostCharactersCharacterIdMailRecipientRecipientTypeEnum[keyof typeof PostCharactersCharacterIdMailRecipientRecipientTypeEnum];


/**
 * Check if a given object implements the PostCharactersCharacterIdMailRecipient interface.
 */
export function instanceOfPostCharactersCharacterIdMailRecipient(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "recipientId" in value;
    isInstance = isInstance && "recipientType" in value;

    return isInstance;
}

export function PostCharactersCharacterIdMailRecipientFromJSON(json: any): PostCharactersCharacterIdMailRecipient {
    return PostCharactersCharacterIdMailRecipientFromJSONTyped(json, false);
}

export function PostCharactersCharacterIdMailRecipientFromJSONTyped(json: any, ignoreDiscriminator: boolean): PostCharactersCharacterIdMailRecipient {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'recipientId': json['recipient_id'],
        'recipientType': json['recipient_type'],
    };
}

export function PostCharactersCharacterIdMailRecipientToJSON(value?: PostCharactersCharacterIdMailRecipient | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'recipient_id': value.recipientId,
        'recipient_type': value.recipientType,
    };
}

