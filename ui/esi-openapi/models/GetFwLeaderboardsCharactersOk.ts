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
import type { GetFwLeaderboardsCharactersKills } from './GetFwLeaderboardsCharactersKills';
import {
    GetFwLeaderboardsCharactersKillsFromJSON,
    GetFwLeaderboardsCharactersKillsFromJSONTyped,
    GetFwLeaderboardsCharactersKillsToJSON,
} from './GetFwLeaderboardsCharactersKills';
import type { GetFwLeaderboardsCharactersVictoryPoints } from './GetFwLeaderboardsCharactersVictoryPoints';
import {
    GetFwLeaderboardsCharactersVictoryPointsFromJSON,
    GetFwLeaderboardsCharactersVictoryPointsFromJSONTyped,
    GetFwLeaderboardsCharactersVictoryPointsToJSON,
} from './GetFwLeaderboardsCharactersVictoryPoints';

/**
 * 200 ok object
 * @export
 * @interface GetFwLeaderboardsCharactersOk
 */
export interface GetFwLeaderboardsCharactersOk {
    /**
     * 
     * @type {GetFwLeaderboardsCharactersKills}
     * @memberof GetFwLeaderboardsCharactersOk
     */
    kills: GetFwLeaderboardsCharactersKills;
    /**
     * 
     * @type {GetFwLeaderboardsCharactersVictoryPoints}
     * @memberof GetFwLeaderboardsCharactersOk
     */
    victoryPoints: GetFwLeaderboardsCharactersVictoryPoints;
}

/**
 * Check if a given object implements the GetFwLeaderboardsCharactersOk interface.
 */
export function instanceOfGetFwLeaderboardsCharactersOk(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "kills" in value;
    isInstance = isInstance && "victoryPoints" in value;

    return isInstance;
}

export function GetFwLeaderboardsCharactersOkFromJSON(json: any): GetFwLeaderboardsCharactersOk {
    return GetFwLeaderboardsCharactersOkFromJSONTyped(json, false);
}

export function GetFwLeaderboardsCharactersOkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetFwLeaderboardsCharactersOk {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'kills': GetFwLeaderboardsCharactersKillsFromJSON(json['kills']),
        'victoryPoints': GetFwLeaderboardsCharactersVictoryPointsFromJSON(json['victory_points']),
    };
}

export function GetFwLeaderboardsCharactersOkToJSON(value?: GetFwLeaderboardsCharactersOk | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'kills': GetFwLeaderboardsCharactersKillsToJSON(value.kills),
        'victory_points': GetFwLeaderboardsCharactersVictoryPointsToJSON(value.victoryPoints),
    };
}

