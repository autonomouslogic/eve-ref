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
import type { GetFwLeaderboardsCharactersActiveTotalActiveTotal } from './GetFwLeaderboardsCharactersActiveTotalActiveTotal';
import {
    GetFwLeaderboardsCharactersActiveTotalActiveTotalFromJSON,
    GetFwLeaderboardsCharactersActiveTotalActiveTotalFromJSONTyped,
    GetFwLeaderboardsCharactersActiveTotalActiveTotalToJSON,
} from './GetFwLeaderboardsCharactersActiveTotalActiveTotal';
import type { GetFwLeaderboardsCharactersLastWeekLastWeek } from './GetFwLeaderboardsCharactersLastWeekLastWeek';
import {
    GetFwLeaderboardsCharactersLastWeekLastWeekFromJSON,
    GetFwLeaderboardsCharactersLastWeekLastWeekFromJSONTyped,
    GetFwLeaderboardsCharactersLastWeekLastWeekToJSON,
} from './GetFwLeaderboardsCharactersLastWeekLastWeek';
import type { GetFwLeaderboardsCharactersYesterdayYesterday } from './GetFwLeaderboardsCharactersYesterdayYesterday';
import {
    GetFwLeaderboardsCharactersYesterdayYesterdayFromJSON,
    GetFwLeaderboardsCharactersYesterdayYesterdayFromJSONTyped,
    GetFwLeaderboardsCharactersYesterdayYesterdayToJSON,
} from './GetFwLeaderboardsCharactersYesterdayYesterday';

/**
 * Top 100 rankings of pilots by number of kills from yesterday, last week and in total
 * @export
 * @interface GetFwLeaderboardsCharactersKills
 */
export interface GetFwLeaderboardsCharactersKills {
    /**
     * Top 100 ranking of pilots active in faction warfare by total kills. A pilot is considered "active" if they have participated in faction warfare in the past 14 days
     * @type {Array<GetFwLeaderboardsCharactersActiveTotalActiveTotal>}
     * @memberof GetFwLeaderboardsCharactersKills
     */
    activeTotal: Array<GetFwLeaderboardsCharactersActiveTotalActiveTotal>;
    /**
     * Top 100 ranking of pilots by kills in the past week
     * @type {Array<GetFwLeaderboardsCharactersLastWeekLastWeek>}
     * @memberof GetFwLeaderboardsCharactersKills
     */
    lastWeek: Array<GetFwLeaderboardsCharactersLastWeekLastWeek>;
    /**
     * Top 100 ranking of pilots by kills in the past day
     * @type {Array<GetFwLeaderboardsCharactersYesterdayYesterday>}
     * @memberof GetFwLeaderboardsCharactersKills
     */
    yesterday: Array<GetFwLeaderboardsCharactersYesterdayYesterday>;
}

/**
 * Check if a given object implements the GetFwLeaderboardsCharactersKills interface.
 */
export function instanceOfGetFwLeaderboardsCharactersKills(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "activeTotal" in value;
    isInstance = isInstance && "lastWeek" in value;
    isInstance = isInstance && "yesterday" in value;

    return isInstance;
}

export function GetFwLeaderboardsCharactersKillsFromJSON(json: any): GetFwLeaderboardsCharactersKills {
    return GetFwLeaderboardsCharactersKillsFromJSONTyped(json, false);
}

export function GetFwLeaderboardsCharactersKillsFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetFwLeaderboardsCharactersKills {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'activeTotal': ((json['active_total'] as Array<any>).map(GetFwLeaderboardsCharactersActiveTotalActiveTotalFromJSON)),
        'lastWeek': ((json['last_week'] as Array<any>).map(GetFwLeaderboardsCharactersLastWeekLastWeekFromJSON)),
        'yesterday': ((json['yesterday'] as Array<any>).map(GetFwLeaderboardsCharactersYesterdayYesterdayFromJSON)),
    };
}

export function GetFwLeaderboardsCharactersKillsToJSON(value?: GetFwLeaderboardsCharactersKills | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'active_total': ((value.activeTotal as Array<any>).map(GetFwLeaderboardsCharactersActiveTotalActiveTotalToJSON)),
        'last_week': ((value.lastWeek as Array<any>).map(GetFwLeaderboardsCharactersLastWeekLastWeekToJSON)),
        'yesterday': ((value.yesterday as Array<any>).map(GetFwLeaderboardsCharactersYesterdayYesterdayToJSON)),
    };
}

