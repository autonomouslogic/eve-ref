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
 * 200 ok object
 * @export
 * @interface GetCharactersCharacterIdNotificationsContacts200Ok
 */
export interface GetCharactersCharacterIdNotificationsContacts200Ok {
    /**
     * message string
     * @type {string}
     * @memberof GetCharactersCharacterIdNotificationsContacts200Ok
     */
    message: string;
    /**
     * notification_id integer
     * @type {number}
     * @memberof GetCharactersCharacterIdNotificationsContacts200Ok
     */
    notificationId: number;
    /**
     * send_date string
     * @type {Date}
     * @memberof GetCharactersCharacterIdNotificationsContacts200Ok
     */
    sendDate: Date;
    /**
     * sender_character_id integer
     * @type {number}
     * @memberof GetCharactersCharacterIdNotificationsContacts200Ok
     */
    senderCharacterId: number;
    /**
     * A number representing the standing level the receiver has been added at by the sender. The standing levels are as follows: -10 -> Terrible | -5 -> Bad |  0 -> Neutral |  5 -> Good |  10 -> Excellent
     * @type {number}
     * @memberof GetCharactersCharacterIdNotificationsContacts200Ok
     */
    standingLevel: number;
}

/**
 * Check if a given object implements the GetCharactersCharacterIdNotificationsContacts200Ok interface.
 */
export function instanceOfGetCharactersCharacterIdNotificationsContacts200Ok(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "message" in value;
    isInstance = isInstance && "notificationId" in value;
    isInstance = isInstance && "sendDate" in value;
    isInstance = isInstance && "senderCharacterId" in value;
    isInstance = isInstance && "standingLevel" in value;

    return isInstance;
}

export function GetCharactersCharacterIdNotificationsContacts200OkFromJSON(json: any): GetCharactersCharacterIdNotificationsContacts200Ok {
    return GetCharactersCharacterIdNotificationsContacts200OkFromJSONTyped(json, false);
}

export function GetCharactersCharacterIdNotificationsContacts200OkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCharactersCharacterIdNotificationsContacts200Ok {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'message': json['message'],
        'notificationId': json['notification_id'],
        'sendDate': (new Date(json['send_date'])),
        'senderCharacterId': json['sender_character_id'],
        'standingLevel': json['standing_level'],
    };
}

export function GetCharactersCharacterIdNotificationsContacts200OkToJSON(value?: GetCharactersCharacterIdNotificationsContacts200Ok | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'message': value.message,
        'notification_id': value.notificationId,
        'send_date': (value.sendDate.toISOString()),
        'sender_character_id': value.senderCharacterId,
        'standing_level': value.standingLevel,
    };
}

