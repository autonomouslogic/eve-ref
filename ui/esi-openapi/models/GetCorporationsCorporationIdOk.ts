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
 * 200 ok object
 * @export
 * @interface GetCorporationsCorporationIdOk
 */
export interface GetCorporationsCorporationIdOk {
    /**
     * ID of the alliance that corporation is a member of, if any
     * @type {number}
     * @memberof GetCorporationsCorporationIdOk
     */
    allianceId?: number;
    /**
     * ceo_id integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdOk
     */
    ceoId: number;
    /**
     * creator_id integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdOk
     */
    creatorId: number;
    /**
     * date_founded string
     * @type {Date}
     * @memberof GetCorporationsCorporationIdOk
     */
    dateFounded?: Date;
    /**
     * description string
     * @type {string}
     * @memberof GetCorporationsCorporationIdOk
     */
    description?: string;
    /**
     * faction_id integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdOk
     */
    factionId?: number;
    /**
     * home_station_id integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdOk
     */
    homeStationId?: number;
    /**
     * member_count integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdOk
     */
    memberCount: number;
    /**
     * the full name of the corporation
     * @type {string}
     * @memberof GetCorporationsCorporationIdOk
     */
    name: string;
    /**
     * shares integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdOk
     */
    shares?: number;
    /**
     * tax_rate number
     * @type {number}
     * @memberof GetCorporationsCorporationIdOk
     */
    taxRate: number;
    /**
     * the short name of the corporation
     * @type {string}
     * @memberof GetCorporationsCorporationIdOk
     */
    ticker: string;
    /**
     * url string
     * @type {string}
     * @memberof GetCorporationsCorporationIdOk
     */
    url?: string;
    /**
     * war_eligible boolean
     * @type {boolean}
     * @memberof GetCorporationsCorporationIdOk
     */
    warEligible?: boolean;
}

/**
 * Check if a given object implements the GetCorporationsCorporationIdOk interface.
 */
export function instanceOfGetCorporationsCorporationIdOk(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "ceoId" in value;
    isInstance = isInstance && "creatorId" in value;
    isInstance = isInstance && "memberCount" in value;
    isInstance = isInstance && "name" in value;
    isInstance = isInstance && "taxRate" in value;
    isInstance = isInstance && "ticker" in value;

    return isInstance;
}

export function GetCorporationsCorporationIdOkFromJSON(json: any): GetCorporationsCorporationIdOk {
    return GetCorporationsCorporationIdOkFromJSONTyped(json, false);
}

export function GetCorporationsCorporationIdOkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCorporationsCorporationIdOk {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'allianceId': !exists(json, 'alliance_id') ? undefined : json['alliance_id'],
        'ceoId': json['ceo_id'],
        'creatorId': json['creator_id'],
        'dateFounded': !exists(json, 'date_founded') ? undefined : (new Date(json['date_founded'])),
        'description': !exists(json, 'description') ? undefined : json['description'],
        'factionId': !exists(json, 'faction_id') ? undefined : json['faction_id'],
        'homeStationId': !exists(json, 'home_station_id') ? undefined : json['home_station_id'],
        'memberCount': json['member_count'],
        'name': json['name'],
        'shares': !exists(json, 'shares') ? undefined : json['shares'],
        'taxRate': json['tax_rate'],
        'ticker': json['ticker'],
        'url': !exists(json, 'url') ? undefined : json['url'],
        'warEligible': !exists(json, 'war_eligible') ? undefined : json['war_eligible'],
    };
}

export function GetCorporationsCorporationIdOkToJSON(value?: GetCorporationsCorporationIdOk | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'alliance_id': value.allianceId,
        'ceo_id': value.ceoId,
        'creator_id': value.creatorId,
        'date_founded': value.dateFounded === undefined ? undefined : (value.dateFounded.toISOString()),
        'description': value.description,
        'faction_id': value.factionId,
        'home_station_id': value.homeStationId,
        'member_count': value.memberCount,
        'name': value.name,
        'shares': value.shares,
        'tax_rate': value.taxRate,
        'ticker': value.ticker,
        'url': value.url,
        'war_eligible': value.warEligible,
    };
}
