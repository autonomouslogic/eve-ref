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
/**
 * 200 ok object
 * @export
 * @interface GetCorporationsCorporationIdStandings200Ok
 */
export interface GetCorporationsCorporationIdStandings200Ok {
    /**
     * from_id integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdStandings200Ok
     */
    fromId: number;
    /**
     * from_type string
     * @type {string}
     * @memberof GetCorporationsCorporationIdStandings200Ok
     */
    fromType: GetCorporationsCorporationIdStandings200OkFromTypeEnum;
    /**
     * standing number
     * @type {number}
     * @memberof GetCorporationsCorporationIdStandings200Ok
     */
    standing: number;
}


/**
 * @export
 */
export const GetCorporationsCorporationIdStandings200OkFromTypeEnum = {
    Agent: 'agent',
    NpcCorp: 'npc_corp',
    Faction: 'faction'
} as const;
export type GetCorporationsCorporationIdStandings200OkFromTypeEnum = typeof GetCorporationsCorporationIdStandings200OkFromTypeEnum[keyof typeof GetCorporationsCorporationIdStandings200OkFromTypeEnum];


/**
 * Check if a given object implements the GetCorporationsCorporationIdStandings200Ok interface.
 */
export function instanceOfGetCorporationsCorporationIdStandings200Ok(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "fromId" in value;
    isInstance = isInstance && "fromType" in value;
    isInstance = isInstance && "standing" in value;

    return isInstance;
}

export function GetCorporationsCorporationIdStandings200OkFromJSON(json: any): GetCorporationsCorporationIdStandings200Ok {
    return GetCorporationsCorporationIdStandings200OkFromJSONTyped(json, false);
}

export function GetCorporationsCorporationIdStandings200OkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCorporationsCorporationIdStandings200Ok {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'fromId': json['from_id'],
        'fromType': json['from_type'],
        'standing': json['standing'],
    };
}

export function GetCorporationsCorporationIdStandings200OkToJSON(value?: GetCorporationsCorporationIdStandings200Ok | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'from_id': value.fromId,
        'from_type': value.fromType,
        'standing': value.standing,
    };
}

