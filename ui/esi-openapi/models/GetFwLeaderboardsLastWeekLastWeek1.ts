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
 * last_week object
 * @export
 * @interface GetFwLeaderboardsLastWeekLastWeek1
 */
export interface GetFwLeaderboardsLastWeekLastWeek1 {
    /**
     * Amount of victory points
     * @type {number}
     * @memberof GetFwLeaderboardsLastWeekLastWeek1
     */
    amount?: number;
    /**
     * faction_id integer
     * @type {number}
     * @memberof GetFwLeaderboardsLastWeekLastWeek1
     */
    factionId?: number;
}

/**
 * Check if a given object implements the GetFwLeaderboardsLastWeekLastWeek1 interface.
 */
export function instanceOfGetFwLeaderboardsLastWeekLastWeek1(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetFwLeaderboardsLastWeekLastWeek1FromJSON(json: any): GetFwLeaderboardsLastWeekLastWeek1 {
    return GetFwLeaderboardsLastWeekLastWeek1FromJSONTyped(json, false);
}

export function GetFwLeaderboardsLastWeekLastWeek1FromJSONTyped(json: any, ignoreDiscriminator: boolean): GetFwLeaderboardsLastWeekLastWeek1 {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'amount': !exists(json, 'amount') ? undefined : json['amount'],
        'factionId': !exists(json, 'faction_id') ? undefined : json['faction_id'],
    };
}

export function GetFwLeaderboardsLastWeekLastWeek1ToJSON(value?: GetFwLeaderboardsLastWeekLastWeek1 | null): any {
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

