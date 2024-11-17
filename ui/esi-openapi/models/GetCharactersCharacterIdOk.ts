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
 * 200 ok object
 * @export
 * @interface GetCharactersCharacterIdOk
 */
export interface GetCharactersCharacterIdOk {
    /**
     * The character's alliance ID
     * @type {number}
     * @memberof GetCharactersCharacterIdOk
     */
    allianceId?: number;
    /**
     * Creation date of the character
     * @type {Date}
     * @memberof GetCharactersCharacterIdOk
     */
    birthday: Date;
    /**
     * bloodline_id integer
     * @type {number}
     * @memberof GetCharactersCharacterIdOk
     */
    bloodlineId: number;
    /**
     * The character's corporation ID
     * @type {number}
     * @memberof GetCharactersCharacterIdOk
     */
    corporationId: number;
    /**
     * description string
     * @type {string}
     * @memberof GetCharactersCharacterIdOk
     */
    description?: string;
    /**
     * ID of the faction the character is fighting for, if the character is enlisted in Factional Warfare
     * @type {number}
     * @memberof GetCharactersCharacterIdOk
     */
    factionId?: number;
    /**
     * gender string
     * @type {string}
     * @memberof GetCharactersCharacterIdOk
     */
    gender: GetCharactersCharacterIdOkGenderEnum;
    /**
     * name string
     * @type {string}
     * @memberof GetCharactersCharacterIdOk
     */
    name: string;
    /**
     * race_id integer
     * @type {number}
     * @memberof GetCharactersCharacterIdOk
     */
    raceId: number;
    /**
     * security_status number
     * @type {number}
     * @memberof GetCharactersCharacterIdOk
     */
    securityStatus?: number;
    /**
     * The individual title of the character
     * @type {string}
     * @memberof GetCharactersCharacterIdOk
     */
    title?: string;
}


/**
 * @export
 */
export const GetCharactersCharacterIdOkGenderEnum = {
    Female: 'female',
    Male: 'male'
} as const;
export type GetCharactersCharacterIdOkGenderEnum = typeof GetCharactersCharacterIdOkGenderEnum[keyof typeof GetCharactersCharacterIdOkGenderEnum];


/**
 * Check if a given object implements the GetCharactersCharacterIdOk interface.
 */
export function instanceOfGetCharactersCharacterIdOk(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "birthday" in value;
    isInstance = isInstance && "bloodlineId" in value;
    isInstance = isInstance && "corporationId" in value;
    isInstance = isInstance && "gender" in value;
    isInstance = isInstance && "name" in value;
    isInstance = isInstance && "raceId" in value;

    return isInstance;
}

export function GetCharactersCharacterIdOkFromJSON(json: any): GetCharactersCharacterIdOk {
    return GetCharactersCharacterIdOkFromJSONTyped(json, false);
}

export function GetCharactersCharacterIdOkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCharactersCharacterIdOk {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'allianceId': !exists(json, 'alliance_id') ? undefined : json['alliance_id'],
        'birthday': (new Date(json['birthday'])),
        'bloodlineId': json['bloodline_id'],
        'corporationId': json['corporation_id'],
        'description': !exists(json, 'description') ? undefined : json['description'],
        'factionId': !exists(json, 'faction_id') ? undefined : json['faction_id'],
        'gender': json['gender'],
        'name': json['name'],
        'raceId': json['race_id'],
        'securityStatus': !exists(json, 'security_status') ? undefined : json['security_status'],
        'title': !exists(json, 'title') ? undefined : json['title'],
    };
}

export function GetCharactersCharacterIdOkToJSON(value?: GetCharactersCharacterIdOk | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'alliance_id': value.allianceId,
        'birthday': (value.birthday.toISOString()),
        'bloodline_id': value.bloodlineId,
        'corporation_id': value.corporationId,
        'description': value.description,
        'faction_id': value.factionId,
        'gender': value.gender,
        'name': value.name,
        'race_id': value.raceId,
        'security_status': value.securityStatus,
        'title': value.title,
    };
}

