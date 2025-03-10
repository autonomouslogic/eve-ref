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
 * @interface GetCorporationsCorporationIdMembertracking200Ok
 */
export interface GetCorporationsCorporationIdMembertracking200Ok {
    /**
     * base_id integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdMembertracking200Ok
     */
    baseId?: number;
    /**
     * character_id integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdMembertracking200Ok
     */
    characterId: number;
    /**
     * location_id integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdMembertracking200Ok
     */
    locationId?: number;
    /**
     * logoff_date string
     * @type {Date}
     * @memberof GetCorporationsCorporationIdMembertracking200Ok
     */
    logoffDate?: Date;
    /**
     * logon_date string
     * @type {Date}
     * @memberof GetCorporationsCorporationIdMembertracking200Ok
     */
    logonDate?: Date;
    /**
     * ship_type_id integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdMembertracking200Ok
     */
    shipTypeId?: number;
    /**
     * start_date string
     * @type {Date}
     * @memberof GetCorporationsCorporationIdMembertracking200Ok
     */
    startDate?: Date;
}

/**
 * Check if a given object implements the GetCorporationsCorporationIdMembertracking200Ok interface.
 */
export function instanceOfGetCorporationsCorporationIdMembertracking200Ok(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "characterId" in value;

    return isInstance;
}

export function GetCorporationsCorporationIdMembertracking200OkFromJSON(json: any): GetCorporationsCorporationIdMembertracking200Ok {
    return GetCorporationsCorporationIdMembertracking200OkFromJSONTyped(json, false);
}

export function GetCorporationsCorporationIdMembertracking200OkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCorporationsCorporationIdMembertracking200Ok {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'baseId': !exists(json, 'base_id') ? undefined : json['base_id'],
        'characterId': json['character_id'],
        'locationId': !exists(json, 'location_id') ? undefined : json['location_id'],
        'logoffDate': !exists(json, 'logoff_date') ? undefined : (new Date(json['logoff_date'])),
        'logonDate': !exists(json, 'logon_date') ? undefined : (new Date(json['logon_date'])),
        'shipTypeId': !exists(json, 'ship_type_id') ? undefined : json['ship_type_id'],
        'startDate': !exists(json, 'start_date') ? undefined : (new Date(json['start_date'])),
    };
}

export function GetCorporationsCorporationIdMembertracking200OkToJSON(value?: GetCorporationsCorporationIdMembertracking200Ok | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'base_id': value.baseId,
        'character_id': value.characterId,
        'location_id': value.locationId,
        'logoff_date': value.logoffDate === undefined ? undefined : (value.logoffDate.toISOString()),
        'logon_date': value.logonDate === undefined ? undefined : (value.logonDate.toISOString()),
        'ship_type_id': value.shipTypeId,
        'start_date': value.startDate === undefined ? undefined : (value.startDate.toISOString()),
    };
}

