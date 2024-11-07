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
import type { GetFwLeaderboardsCorporationsActiveTotalActiveTotal } from './GetFwLeaderboardsCorporationsActiveTotalActiveTotal';
import {
    GetFwLeaderboardsCorporationsActiveTotalActiveTotalFromJSON,
    GetFwLeaderboardsCorporationsActiveTotalActiveTotalFromJSONTyped,
    GetFwLeaderboardsCorporationsActiveTotalActiveTotalToJSON,
} from './GetFwLeaderboardsCorporationsActiveTotalActiveTotal';
import type { GetFwLeaderboardsCorporationsLastWeekLastWeek } from './GetFwLeaderboardsCorporationsLastWeekLastWeek';
import {
    GetFwLeaderboardsCorporationsLastWeekLastWeekFromJSON,
    GetFwLeaderboardsCorporationsLastWeekLastWeekFromJSONTyped,
    GetFwLeaderboardsCorporationsLastWeekLastWeekToJSON,
} from './GetFwLeaderboardsCorporationsLastWeekLastWeek';
import type { GetFwLeaderboardsCorporationsYesterdayYesterday } from './GetFwLeaderboardsCorporationsYesterdayYesterday';
import {
    GetFwLeaderboardsCorporationsYesterdayYesterdayFromJSON,
    GetFwLeaderboardsCorporationsYesterdayYesterdayFromJSONTyped,
    GetFwLeaderboardsCorporationsYesterdayYesterdayToJSON,
} from './GetFwLeaderboardsCorporationsYesterdayYesterday';

/**
 * Top 10 rankings of corporations by number of kills from yesterday, last week and in total
 * @export
 * @interface GetFwLeaderboardsCorporationsKills
 */
export interface GetFwLeaderboardsCorporationsKills {
    /**
     * Top 10 ranking of corporations active in faction warfare by total kills. A corporation is considered "active" if they have participated in faction warfare in the past 14 days
     * @type {Array<GetFwLeaderboardsCorporationsActiveTotalActiveTotal>}
     * @memberof GetFwLeaderboardsCorporationsKills
     */
    activeTotal: Array<GetFwLeaderboardsCorporationsActiveTotalActiveTotal>;
    /**
     * Top 10 ranking of corporations by kills in the past week
     * @type {Array<GetFwLeaderboardsCorporationsLastWeekLastWeek>}
     * @memberof GetFwLeaderboardsCorporationsKills
     */
    lastWeek: Array<GetFwLeaderboardsCorporationsLastWeekLastWeek>;
    /**
     * Top 10 ranking of corporations by kills in the past day
     * @type {Array<GetFwLeaderboardsCorporationsYesterdayYesterday>}
     * @memberof GetFwLeaderboardsCorporationsKills
     */
    yesterday: Array<GetFwLeaderboardsCorporationsYesterdayYesterday>;
}

/**
 * Check if a given object implements the GetFwLeaderboardsCorporationsKills interface.
 */
export function instanceOfGetFwLeaderboardsCorporationsKills(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "activeTotal" in value;
    isInstance = isInstance && "lastWeek" in value;
    isInstance = isInstance && "yesterday" in value;

    return isInstance;
}

export function GetFwLeaderboardsCorporationsKillsFromJSON(json: any): GetFwLeaderboardsCorporationsKills {
    return GetFwLeaderboardsCorporationsKillsFromJSONTyped(json, false);
}

export function GetFwLeaderboardsCorporationsKillsFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetFwLeaderboardsCorporationsKills {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'activeTotal': ((json['active_total'] as Array<any>).map(GetFwLeaderboardsCorporationsActiveTotalActiveTotalFromJSON)),
        'lastWeek': ((json['last_week'] as Array<any>).map(GetFwLeaderboardsCorporationsLastWeekLastWeekFromJSON)),
        'yesterday': ((json['yesterday'] as Array<any>).map(GetFwLeaderboardsCorporationsYesterdayYesterdayFromJSON)),
    };
}

export function GetFwLeaderboardsCorporationsKillsToJSON(value?: GetFwLeaderboardsCorporationsKills | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'active_total': ((value.activeTotal as Array<any>).map(GetFwLeaderboardsCorporationsActiveTotalActiveTotalToJSON)),
        'last_week': ((value.lastWeek as Array<any>).map(GetFwLeaderboardsCorporationsLastWeekLastWeekToJSON)),
        'yesterday': ((value.yesterday as Array<any>).map(GetFwLeaderboardsCorporationsYesterdayYesterdayToJSON)),
    };
}

