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
 * last_week object
 * @export
 * @interface GetFwLeaderboardsCharactersLastWeekLastWeek
 */
export interface GetFwLeaderboardsCharactersLastWeekLastWeek {
    /**
     * Amount of kills
     * @type {number}
     * @memberof GetFwLeaderboardsCharactersLastWeekLastWeek
     */
    amount?: number;
    /**
     * character_id integer
     * @type {number}
     * @memberof GetFwLeaderboardsCharactersLastWeekLastWeek
     */
    characterId?: number;
}

/**
 * Check if a given object implements the GetFwLeaderboardsCharactersLastWeekLastWeek interface.
 */
export function instanceOfGetFwLeaderboardsCharactersLastWeekLastWeek(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetFwLeaderboardsCharactersLastWeekLastWeekFromJSON(json: any): GetFwLeaderboardsCharactersLastWeekLastWeek {
    return GetFwLeaderboardsCharactersLastWeekLastWeekFromJSONTyped(json, false);
}

export function GetFwLeaderboardsCharactersLastWeekLastWeekFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetFwLeaderboardsCharactersLastWeekLastWeek {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'amount': !exists(json, 'amount') ? undefined : json['amount'],
        'characterId': !exists(json, 'character_id') ? undefined : json['character_id'],
    };
}

export function GetFwLeaderboardsCharactersLastWeekLastWeekToJSON(value?: GetFwLeaderboardsCharactersLastWeekLastWeek | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'amount': value.amount,
        'character_id': value.characterId,
    };
}

