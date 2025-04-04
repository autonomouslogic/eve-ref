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
import type { GetFwLeaderboardsKills } from './GetFwLeaderboardsKills';
import {
    GetFwLeaderboardsKillsFromJSON,
    GetFwLeaderboardsKillsFromJSONTyped,
    GetFwLeaderboardsKillsToJSON,
} from './GetFwLeaderboardsKills';
import type { GetFwLeaderboardsVictoryPoints } from './GetFwLeaderboardsVictoryPoints';
import {
    GetFwLeaderboardsVictoryPointsFromJSON,
    GetFwLeaderboardsVictoryPointsFromJSONTyped,
    GetFwLeaderboardsVictoryPointsToJSON,
} from './GetFwLeaderboardsVictoryPoints';

/**
 * 200 ok object
 * @export
 * @interface GetFwLeaderboardsOk
 */
export interface GetFwLeaderboardsOk {
    /**
     * 
     * @type {GetFwLeaderboardsKills}
     * @memberof GetFwLeaderboardsOk
     */
    kills: GetFwLeaderboardsKills;
    /**
     * 
     * @type {GetFwLeaderboardsVictoryPoints}
     * @memberof GetFwLeaderboardsOk
     */
    victoryPoints: GetFwLeaderboardsVictoryPoints;
}

/**
 * Check if a given object implements the GetFwLeaderboardsOk interface.
 */
export function instanceOfGetFwLeaderboardsOk(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "kills" in value;
    isInstance = isInstance && "victoryPoints" in value;

    return isInstance;
}

export function GetFwLeaderboardsOkFromJSON(json: any): GetFwLeaderboardsOk {
    return GetFwLeaderboardsOkFromJSONTyped(json, false);
}

export function GetFwLeaderboardsOkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetFwLeaderboardsOk {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'kills': GetFwLeaderboardsKillsFromJSON(json['kills']),
        'victoryPoints': GetFwLeaderboardsVictoryPointsFromJSON(json['victory_points']),
    };
}

export function GetFwLeaderboardsOkToJSON(value?: GetFwLeaderboardsOk | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'kills': GetFwLeaderboardsKillsToJSON(value.kills),
        'victory_points': GetFwLeaderboardsVictoryPointsToJSON(value.victoryPoints),
    };
}

