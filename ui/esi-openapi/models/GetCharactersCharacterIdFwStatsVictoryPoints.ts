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
 * Summary of victory points gained by the given character for the enlisted faction
 * @export
 * @interface GetCharactersCharacterIdFwStatsVictoryPoints
 */
export interface GetCharactersCharacterIdFwStatsVictoryPoints {
    /**
     * Last week's victory points gained by the given character
     * @type {number}
     * @memberof GetCharactersCharacterIdFwStatsVictoryPoints
     */
    lastWeek: number;
    /**
     * Total victory points gained since the given character enlisted
     * @type {number}
     * @memberof GetCharactersCharacterIdFwStatsVictoryPoints
     */
    total: number;
    /**
     * Yesterday's victory points gained by the given character
     * @type {number}
     * @memberof GetCharactersCharacterIdFwStatsVictoryPoints
     */
    yesterday: number;
}

/**
 * Check if a given object implements the GetCharactersCharacterIdFwStatsVictoryPoints interface.
 */
export function instanceOfGetCharactersCharacterIdFwStatsVictoryPoints(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "lastWeek" in value;
    isInstance = isInstance && "total" in value;
    isInstance = isInstance && "yesterday" in value;

    return isInstance;
}

export function GetCharactersCharacterIdFwStatsVictoryPointsFromJSON(json: any): GetCharactersCharacterIdFwStatsVictoryPoints {
    return GetCharactersCharacterIdFwStatsVictoryPointsFromJSONTyped(json, false);
}

export function GetCharactersCharacterIdFwStatsVictoryPointsFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCharactersCharacterIdFwStatsVictoryPoints {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'lastWeek': json['last_week'],
        'total': json['total'],
        'yesterday': json['yesterday'],
    };
}

export function GetCharactersCharacterIdFwStatsVictoryPointsToJSON(value?: GetCharactersCharacterIdFwStatsVictoryPoints | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'last_week': value.lastWeek,
        'total': value.total,
        'yesterday': value.yesterday,
    };
}
