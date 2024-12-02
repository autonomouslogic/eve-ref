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
 * @interface GetFwLeaderboardsCorporationsLastWeekLastWeek1
 */
export interface GetFwLeaderboardsCorporationsLastWeekLastWeek1 {
    /**
     * Amount of victory points
     * @type {number}
     * @memberof GetFwLeaderboardsCorporationsLastWeekLastWeek1
     */
    amount?: number;
    /**
     * corporation_id integer
     * @type {number}
     * @memberof GetFwLeaderboardsCorporationsLastWeekLastWeek1
     */
    corporationId?: number;
}

/**
 * Check if a given object implements the GetFwLeaderboardsCorporationsLastWeekLastWeek1 interface.
 */
export function instanceOfGetFwLeaderboardsCorporationsLastWeekLastWeek1(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetFwLeaderboardsCorporationsLastWeekLastWeek1FromJSON(json: any): GetFwLeaderboardsCorporationsLastWeekLastWeek1 {
    return GetFwLeaderboardsCorporationsLastWeekLastWeek1FromJSONTyped(json, false);
}

export function GetFwLeaderboardsCorporationsLastWeekLastWeek1FromJSONTyped(json: any, ignoreDiscriminator: boolean): GetFwLeaderboardsCorporationsLastWeekLastWeek1 {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'amount': !exists(json, 'amount') ? undefined : json['amount'],
        'corporationId': !exists(json, 'corporation_id') ? undefined : json['corporation_id'],
    };
}

export function GetFwLeaderboardsCorporationsLastWeekLastWeek1ToJSON(value?: GetFwLeaderboardsCorporationsLastWeekLastWeek1 | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'amount': value.amount,
        'corporation_id': value.corporationId,
    };
}
