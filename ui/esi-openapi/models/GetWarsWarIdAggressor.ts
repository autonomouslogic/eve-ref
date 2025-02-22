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
 * The aggressor corporation or alliance that declared this war, only contains either corporation_id or alliance_id
 * @export
 * @interface GetWarsWarIdAggressor
 */
export interface GetWarsWarIdAggressor {
    /**
     * Alliance ID if and only if the aggressor is an alliance
     * @type {number}
     * @memberof GetWarsWarIdAggressor
     */
    allianceId?: number;
    /**
     * Corporation ID if and only if the aggressor is a corporation
     * @type {number}
     * @memberof GetWarsWarIdAggressor
     */
    corporationId?: number;
    /**
     * ISK value of ships the aggressor has destroyed
     * @type {number}
     * @memberof GetWarsWarIdAggressor
     */
    iskDestroyed: number;
    /**
     * The number of ships the aggressor has killed
     * @type {number}
     * @memberof GetWarsWarIdAggressor
     */
    shipsKilled: number;
}

/**
 * Check if a given object implements the GetWarsWarIdAggressor interface.
 */
export function instanceOfGetWarsWarIdAggressor(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "iskDestroyed" in value;
    isInstance = isInstance && "shipsKilled" in value;

    return isInstance;
}

export function GetWarsWarIdAggressorFromJSON(json: any): GetWarsWarIdAggressor {
    return GetWarsWarIdAggressorFromJSONTyped(json, false);
}

export function GetWarsWarIdAggressorFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetWarsWarIdAggressor {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'allianceId': !exists(json, 'alliance_id') ? undefined : json['alliance_id'],
        'corporationId': !exists(json, 'corporation_id') ? undefined : json['corporation_id'],
        'iskDestroyed': json['isk_destroyed'],
        'shipsKilled': json['ships_killed'],
    };
}

export function GetWarsWarIdAggressorToJSON(value?: GetWarsWarIdAggressor | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'alliance_id': value.allianceId,
        'corporation_id': value.corporationId,
        'isk_destroyed': value.iskDestroyed,
        'ships_killed': value.shipsKilled,
    };
}

