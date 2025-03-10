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
 * participant object
 * @export
 * @interface GetSovereigntyCampaignsParticipant
 */
export interface GetSovereigntyCampaignsParticipant {
    /**
     * alliance_id integer
     * @type {number}
     * @memberof GetSovereigntyCampaignsParticipant
     */
    allianceId: number;
    /**
     * score number
     * @type {number}
     * @memberof GetSovereigntyCampaignsParticipant
     */
    score: number;
}

/**
 * Check if a given object implements the GetSovereigntyCampaignsParticipant interface.
 */
export function instanceOfGetSovereigntyCampaignsParticipant(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "allianceId" in value;
    isInstance = isInstance && "score" in value;

    return isInstance;
}

export function GetSovereigntyCampaignsParticipantFromJSON(json: any): GetSovereigntyCampaignsParticipant {
    return GetSovereigntyCampaignsParticipantFromJSONTyped(json, false);
}

export function GetSovereigntyCampaignsParticipantFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetSovereigntyCampaignsParticipant {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'allianceId': json['alliance_id'],
        'score': json['score'],
    };
}

export function GetSovereigntyCampaignsParticipantToJSON(value?: GetSovereigntyCampaignsParticipant | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'alliance_id': value.allianceId,
        'score': value.score,
    };
}

