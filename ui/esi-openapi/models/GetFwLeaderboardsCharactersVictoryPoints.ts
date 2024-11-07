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
import type { GetFwLeaderboardsCharactersActiveTotalActiveTotal1 } from './GetFwLeaderboardsCharactersActiveTotalActiveTotal1';
import {
    GetFwLeaderboardsCharactersActiveTotalActiveTotal1FromJSON,
    GetFwLeaderboardsCharactersActiveTotalActiveTotal1FromJSONTyped,
    GetFwLeaderboardsCharactersActiveTotalActiveTotal1ToJSON,
} from './GetFwLeaderboardsCharactersActiveTotalActiveTotal1';
import type { GetFwLeaderboardsCharactersLastWeekLastWeek1 } from './GetFwLeaderboardsCharactersLastWeekLastWeek1';
import {
    GetFwLeaderboardsCharactersLastWeekLastWeek1FromJSON,
    GetFwLeaderboardsCharactersLastWeekLastWeek1FromJSONTyped,
    GetFwLeaderboardsCharactersLastWeekLastWeek1ToJSON,
} from './GetFwLeaderboardsCharactersLastWeekLastWeek1';
import type { GetFwLeaderboardsCharactersYesterdayYesterday1 } from './GetFwLeaderboardsCharactersYesterdayYesterday1';
import {
    GetFwLeaderboardsCharactersYesterdayYesterday1FromJSON,
    GetFwLeaderboardsCharactersYesterdayYesterday1FromJSONTyped,
    GetFwLeaderboardsCharactersYesterdayYesterday1ToJSON,
} from './GetFwLeaderboardsCharactersYesterdayYesterday1';

/**
 * Top 100 rankings of pilots by victory points from yesterday, last week and in total
 * @export
 * @interface GetFwLeaderboardsCharactersVictoryPoints
 */
export interface GetFwLeaderboardsCharactersVictoryPoints {
    /**
     * Top 100 ranking of pilots active in faction warfare by total victory points. A pilot is considered "active" if they have participated in faction warfare in the past 14 days
     * @type {Array<GetFwLeaderboardsCharactersActiveTotalActiveTotal1>}
     * @memberof GetFwLeaderboardsCharactersVictoryPoints
     */
    activeTotal: Array<GetFwLeaderboardsCharactersActiveTotalActiveTotal1>;
    /**
     * Top 100 ranking of pilots by victory points in the past week
     * @type {Array<GetFwLeaderboardsCharactersLastWeekLastWeek1>}
     * @memberof GetFwLeaderboardsCharactersVictoryPoints
     */
    lastWeek: Array<GetFwLeaderboardsCharactersLastWeekLastWeek1>;
    /**
     * Top 100 ranking of pilots by victory points in the past day
     * @type {Array<GetFwLeaderboardsCharactersYesterdayYesterday1>}
     * @memberof GetFwLeaderboardsCharactersVictoryPoints
     */
    yesterday: Array<GetFwLeaderboardsCharactersYesterdayYesterday1>;
}

/**
 * Check if a given object implements the GetFwLeaderboardsCharactersVictoryPoints interface.
 */
export function instanceOfGetFwLeaderboardsCharactersVictoryPoints(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "activeTotal" in value;
    isInstance = isInstance && "lastWeek" in value;
    isInstance = isInstance && "yesterday" in value;

    return isInstance;
}

export function GetFwLeaderboardsCharactersVictoryPointsFromJSON(json: any): GetFwLeaderboardsCharactersVictoryPoints {
    return GetFwLeaderboardsCharactersVictoryPointsFromJSONTyped(json, false);
}

export function GetFwLeaderboardsCharactersVictoryPointsFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetFwLeaderboardsCharactersVictoryPoints {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'activeTotal': ((json['active_total'] as Array<any>).map(GetFwLeaderboardsCharactersActiveTotalActiveTotal1FromJSON)),
        'lastWeek': ((json['last_week'] as Array<any>).map(GetFwLeaderboardsCharactersLastWeekLastWeek1FromJSON)),
        'yesterday': ((json['yesterday'] as Array<any>).map(GetFwLeaderboardsCharactersYesterdayYesterday1FromJSON)),
    };
}

export function GetFwLeaderboardsCharactersVictoryPointsToJSON(value?: GetFwLeaderboardsCharactersVictoryPoints | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'active_total': ((value.activeTotal as Array<any>).map(GetFwLeaderboardsCharactersActiveTotalActiveTotal1ToJSON)),
        'last_week': ((value.lastWeek as Array<any>).map(GetFwLeaderboardsCharactersLastWeekLastWeek1ToJSON)),
        'yesterday': ((value.yesterday as Array<any>).map(GetFwLeaderboardsCharactersYesterdayYesterday1ToJSON)),
    };
}

