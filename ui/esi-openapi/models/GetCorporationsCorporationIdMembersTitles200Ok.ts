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
 * @interface GetCorporationsCorporationIdMembersTitles200Ok
 */
export interface GetCorporationsCorporationIdMembersTitles200Ok {
    /**
     * character_id integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdMembersTitles200Ok
     */
    characterId: number;
    /**
     * A list of title_id
     * @type {Array<number>}
     * @memberof GetCorporationsCorporationIdMembersTitles200Ok
     */
    titles: Array<number>;
}

/**
 * Check if a given object implements the GetCorporationsCorporationIdMembersTitles200Ok interface.
 */
export function instanceOfGetCorporationsCorporationIdMembersTitles200Ok(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "characterId" in value;
    isInstance = isInstance && "titles" in value;

    return isInstance;
}

export function GetCorporationsCorporationIdMembersTitles200OkFromJSON(json: any): GetCorporationsCorporationIdMembersTitles200Ok {
    return GetCorporationsCorporationIdMembersTitles200OkFromJSONTyped(json, false);
}

export function GetCorporationsCorporationIdMembersTitles200OkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCorporationsCorporationIdMembersTitles200Ok {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'characterId': json['character_id'],
        'titles': json['titles'],
    };
}

export function GetCorporationsCorporationIdMembersTitles200OkToJSON(value?: GetCorporationsCorporationIdMembersTitles200Ok | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'character_id': value.characterId,
        'titles': value.titles,
    };
}

