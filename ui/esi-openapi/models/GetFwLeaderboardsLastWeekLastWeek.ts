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
 * @interface GetFwLeaderboardsLastWeekLastWeek
 */
export interface GetFwLeaderboardsLastWeekLastWeek {
    /**
     * Amount of kills
     * @type {number}
     * @memberof GetFwLeaderboardsLastWeekLastWeek
     */
    amount?: number;
    /**
     * faction_id integer
     * @type {number}
     * @memberof GetFwLeaderboardsLastWeekLastWeek
     */
    factionId?: number;
}

/**
 * Check if a given object implements the GetFwLeaderboardsLastWeekLastWeek interface.
 */
export function instanceOfGetFwLeaderboardsLastWeekLastWeek(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetFwLeaderboardsLastWeekLastWeekFromJSON(json: any): GetFwLeaderboardsLastWeekLastWeek {
    return GetFwLeaderboardsLastWeekLastWeekFromJSONTyped(json, false);
}

export function GetFwLeaderboardsLastWeekLastWeekFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetFwLeaderboardsLastWeekLastWeek {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'amount': !exists(json, 'amount') ? undefined : json['amount'],
        'factionId': !exists(json, 'faction_id') ? undefined : json['faction_id'],
    };
}

export function GetFwLeaderboardsLastWeekLastWeekToJSON(value?: GetFwLeaderboardsLastWeekLastWeek | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'amount': value.amount,
        'faction_id': value.factionId,
    };
}

