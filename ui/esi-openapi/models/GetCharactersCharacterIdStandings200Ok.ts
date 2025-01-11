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
 * @interface GetCharactersCharacterIdStandings200Ok
 */
export interface GetCharactersCharacterIdStandings200Ok {
    /**
     * from_id integer
     * @type {number}
     * @memberof GetCharactersCharacterIdStandings200Ok
     */
    fromId: number;
    /**
     * from_type string
     * @type {string}
     * @memberof GetCharactersCharacterIdStandings200Ok
     */
    fromType: GetCharactersCharacterIdStandings200OkFromTypeEnum;
    /**
     * standing number
     * @type {number}
     * @memberof GetCharactersCharacterIdStandings200Ok
     */
    standing: number;
}


/**
 * @export
 */
export const GetCharactersCharacterIdStandings200OkFromTypeEnum = {
    Agent: 'agent',
    NpcCorp: 'npc_corp',
    Faction: 'faction'
} as const;
export type GetCharactersCharacterIdStandings200OkFromTypeEnum = typeof GetCharactersCharacterIdStandings200OkFromTypeEnum[keyof typeof GetCharactersCharacterIdStandings200OkFromTypeEnum];


/**
 * Check if a given object implements the GetCharactersCharacterIdStandings200Ok interface.
 */
export function instanceOfGetCharactersCharacterIdStandings200Ok(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "fromId" in value;
    isInstance = isInstance && "fromType" in value;
    isInstance = isInstance && "standing" in value;

    return isInstance;
}

export function GetCharactersCharacterIdStandings200OkFromJSON(json: any): GetCharactersCharacterIdStandings200Ok {
    return GetCharactersCharacterIdStandings200OkFromJSONTyped(json, false);
}

export function GetCharactersCharacterIdStandings200OkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCharactersCharacterIdStandings200Ok {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'fromId': json['from_id'],
        'fromType': json['from_type'],
        'standing': json['standing'],
    };
}

export function GetCharactersCharacterIdStandings200OkToJSON(value?: GetCharactersCharacterIdStandings200Ok | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'from_id': value.fromId,
        'from_type': value.fromType,
        'standing': value.standing,
    };
}

