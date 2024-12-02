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
 * yesterday object
 * @export
 * @interface GetFwLeaderboardsCorporationsYesterdayYesterday1
 */
export interface GetFwLeaderboardsCorporationsYesterdayYesterday1 {
    /**
     * Amount of victory points
     * @type {number}
     * @memberof GetFwLeaderboardsCorporationsYesterdayYesterday1
     */
    amount?: number;
    /**
     * corporation_id integer
     * @type {number}
     * @memberof GetFwLeaderboardsCorporationsYesterdayYesterday1
     */
    corporationId?: number;
}

/**
 * Check if a given object implements the GetFwLeaderboardsCorporationsYesterdayYesterday1 interface.
 */
export function instanceOfGetFwLeaderboardsCorporationsYesterdayYesterday1(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetFwLeaderboardsCorporationsYesterdayYesterday1FromJSON(json: any): GetFwLeaderboardsCorporationsYesterdayYesterday1 {
    return GetFwLeaderboardsCorporationsYesterdayYesterday1FromJSONTyped(json, false);
}

export function GetFwLeaderboardsCorporationsYesterdayYesterday1FromJSONTyped(json: any, ignoreDiscriminator: boolean): GetFwLeaderboardsCorporationsYesterdayYesterday1 {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'amount': !exists(json, 'amount') ? undefined : json['amount'],
        'corporationId': !exists(json, 'corporation_id') ? undefined : json['corporation_id'],
    };
}

export function GetFwLeaderboardsCorporationsYesterdayYesterday1ToJSON(value?: GetFwLeaderboardsCorporationsYesterdayYesterday1 | null): any {
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
