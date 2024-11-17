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
import type { GetCorporationsCorporationIdFwStatsKills } from './GetCorporationsCorporationIdFwStatsKills';
import {
    GetCorporationsCorporationIdFwStatsKillsFromJSON,
    GetCorporationsCorporationIdFwStatsKillsFromJSONTyped,
    GetCorporationsCorporationIdFwStatsKillsToJSON,
} from './GetCorporationsCorporationIdFwStatsKills';
import type { GetCorporationsCorporationIdFwStatsVictoryPoints } from './GetCorporationsCorporationIdFwStatsVictoryPoints';
import {
    GetCorporationsCorporationIdFwStatsVictoryPointsFromJSON,
    GetCorporationsCorporationIdFwStatsVictoryPointsFromJSONTyped,
    GetCorporationsCorporationIdFwStatsVictoryPointsToJSON,
} from './GetCorporationsCorporationIdFwStatsVictoryPoints';

/**
 * 200 ok object
 * @export
 * @interface GetCorporationsCorporationIdFwStatsOk
 */
export interface GetCorporationsCorporationIdFwStatsOk {
    /**
     * The enlistment date of the given corporation into faction warfare. Will not be included if corporation is not enlisted in faction warfare
     * @type {Date}
     * @memberof GetCorporationsCorporationIdFwStatsOk
     */
    enlistedOn?: Date;
    /**
     * The faction the given corporation is enlisted to fight for. Will not be included if corporation is not enlisted in faction warfare
     * @type {number}
     * @memberof GetCorporationsCorporationIdFwStatsOk
     */
    factionId?: number;
    /**
     * 
     * @type {GetCorporationsCorporationIdFwStatsKills}
     * @memberof GetCorporationsCorporationIdFwStatsOk
     */
    kills: GetCorporationsCorporationIdFwStatsKills;
    /**
     * How many pilots the enlisted corporation has. Will not be included if corporation is not enlisted in faction warfare
     * @type {number}
     * @memberof GetCorporationsCorporationIdFwStatsOk
     */
    pilots?: number;
    /**
     * 
     * @type {GetCorporationsCorporationIdFwStatsVictoryPoints}
     * @memberof GetCorporationsCorporationIdFwStatsOk
     */
    victoryPoints: GetCorporationsCorporationIdFwStatsVictoryPoints;
}

/**
 * Check if a given object implements the GetCorporationsCorporationIdFwStatsOk interface.
 */
export function instanceOfGetCorporationsCorporationIdFwStatsOk(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "kills" in value;
    isInstance = isInstance && "victoryPoints" in value;

    return isInstance;
}

export function GetCorporationsCorporationIdFwStatsOkFromJSON(json: any): GetCorporationsCorporationIdFwStatsOk {
    return GetCorporationsCorporationIdFwStatsOkFromJSONTyped(json, false);
}

export function GetCorporationsCorporationIdFwStatsOkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCorporationsCorporationIdFwStatsOk {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'enlistedOn': !exists(json, 'enlisted_on') ? undefined : (new Date(json['enlisted_on'])),
        'factionId': !exists(json, 'faction_id') ? undefined : json['faction_id'],
        'kills': GetCorporationsCorporationIdFwStatsKillsFromJSON(json['kills']),
        'pilots': !exists(json, 'pilots') ? undefined : json['pilots'],
        'victoryPoints': GetCorporationsCorporationIdFwStatsVictoryPointsFromJSON(json['victory_points']),
    };
}

export function GetCorporationsCorporationIdFwStatsOkToJSON(value?: GetCorporationsCorporationIdFwStatsOk | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'enlisted_on': value.enlistedOn === undefined ? undefined : (value.enlistedOn.toISOString()),
        'faction_id': value.factionId,
        'kills': GetCorporationsCorporationIdFwStatsKillsToJSON(value.kills),
        'pilots': value.pilots,
        'victory_points': GetCorporationsCorporationIdFwStatsVictoryPointsToJSON(value.victoryPoints),
    };
}

