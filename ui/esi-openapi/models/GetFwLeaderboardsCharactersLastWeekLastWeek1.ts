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
 * @interface GetFwLeaderboardsCharactersLastWeekLastWeek1
 */
export interface GetFwLeaderboardsCharactersLastWeekLastWeek1 {
    /**
     * Amount of victory points
     * @type {number}
     * @memberof GetFwLeaderboardsCharactersLastWeekLastWeek1
     */
    amount?: number;
    /**
     * character_id integer
     * @type {number}
     * @memberof GetFwLeaderboardsCharactersLastWeekLastWeek1
     */
    characterId?: number;
}

/**
 * Check if a given object implements the GetFwLeaderboardsCharactersLastWeekLastWeek1 interface.
 */
export function instanceOfGetFwLeaderboardsCharactersLastWeekLastWeek1(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetFwLeaderboardsCharactersLastWeekLastWeek1FromJSON(json: any): GetFwLeaderboardsCharactersLastWeekLastWeek1 {
    return GetFwLeaderboardsCharactersLastWeekLastWeek1FromJSONTyped(json, false);
}

export function GetFwLeaderboardsCharactersLastWeekLastWeek1FromJSONTyped(json: any, ignoreDiscriminator: boolean): GetFwLeaderboardsCharactersLastWeekLastWeek1 {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'amount': !exists(json, 'amount') ? undefined : json['amount'],
        'characterId': !exists(json, 'character_id') ? undefined : json['character_id'],
    };
}

export function GetFwLeaderboardsCharactersLastWeekLastWeek1ToJSON(value?: GetFwLeaderboardsCharactersLastWeekLastWeek1 | null): any {
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

