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
 * @interface GetCorporationsCorporationIdShareholders200Ok
 */
export interface GetCorporationsCorporationIdShareholders200Ok {
    /**
     * share_count integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdShareholders200Ok
     */
    shareCount: number;
    /**
     * shareholder_id integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdShareholders200Ok
     */
    shareholderId: number;
    /**
     * shareholder_type string
     * @type {string}
     * @memberof GetCorporationsCorporationIdShareholders200Ok
     */
    shareholderType: GetCorporationsCorporationIdShareholders200OkShareholderTypeEnum;
}


/**
 * @export
 */
export const GetCorporationsCorporationIdShareholders200OkShareholderTypeEnum = {
    Character: 'character',
    Corporation: 'corporation'
} as const;
export type GetCorporationsCorporationIdShareholders200OkShareholderTypeEnum = typeof GetCorporationsCorporationIdShareholders200OkShareholderTypeEnum[keyof typeof GetCorporationsCorporationIdShareholders200OkShareholderTypeEnum];


/**
 * Check if a given object implements the GetCorporationsCorporationIdShareholders200Ok interface.
 */
export function instanceOfGetCorporationsCorporationIdShareholders200Ok(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "shareCount" in value;
    isInstance = isInstance && "shareholderId" in value;
    isInstance = isInstance && "shareholderType" in value;

    return isInstance;
}

export function GetCorporationsCorporationIdShareholders200OkFromJSON(json: any): GetCorporationsCorporationIdShareholders200Ok {
    return GetCorporationsCorporationIdShareholders200OkFromJSONTyped(json, false);
}

export function GetCorporationsCorporationIdShareholders200OkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCorporationsCorporationIdShareholders200Ok {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'shareCount': json['share_count'],
        'shareholderId': json['shareholder_id'],
        'shareholderType': json['shareholder_type'],
    };
}

export function GetCorporationsCorporationIdShareholders200OkToJSON(value?: GetCorporationsCorporationIdShareholders200Ok | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'share_count': value.shareCount,
        'shareholder_id': value.shareholderId,
        'shareholder_type': value.shareholderType,
    };
}

