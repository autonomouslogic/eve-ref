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
 * active_total object
 * @export
 * @interface GetFwLeaderboardsActiveTotalActiveTotal
 */
export interface GetFwLeaderboardsActiveTotalActiveTotal {
    /**
     * Amount of kills
     * @type {number}
     * @memberof GetFwLeaderboardsActiveTotalActiveTotal
     */
    amount?: number;
    /**
     * faction_id integer
     * @type {number}
     * @memberof GetFwLeaderboardsActiveTotalActiveTotal
     */
    factionId?: number;
}

/**
 * Check if a given object implements the GetFwLeaderboardsActiveTotalActiveTotal interface.
 */
export function instanceOfGetFwLeaderboardsActiveTotalActiveTotal(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetFwLeaderboardsActiveTotalActiveTotalFromJSON(json: any): GetFwLeaderboardsActiveTotalActiveTotal {
    return GetFwLeaderboardsActiveTotalActiveTotalFromJSONTyped(json, false);
}

export function GetFwLeaderboardsActiveTotalActiveTotalFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetFwLeaderboardsActiveTotalActiveTotal {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'amount': !exists(json, 'amount') ? undefined : json['amount'],
        'factionId': !exists(json, 'faction_id') ? undefined : json['faction_id'],
    };
}

export function GetFwLeaderboardsActiveTotalActiveTotalToJSON(value?: GetFwLeaderboardsActiveTotalActiveTotal | null): any {
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

