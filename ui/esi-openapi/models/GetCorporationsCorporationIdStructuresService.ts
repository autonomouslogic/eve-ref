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
 * service object
 * @export
 * @interface GetCorporationsCorporationIdStructuresService
 */
export interface GetCorporationsCorporationIdStructuresService {
    /**
     * name string
     * @type {string}
     * @memberof GetCorporationsCorporationIdStructuresService
     */
    name: string;
    /**
     * state string
     * @type {string}
     * @memberof GetCorporationsCorporationIdStructuresService
     */
    state: GetCorporationsCorporationIdStructuresServiceStateEnum;
}


/**
 * @export
 */
export const GetCorporationsCorporationIdStructuresServiceStateEnum = {
    Online: 'online',
    Offline: 'offline',
    Cleanup: 'cleanup'
} as const;
export type GetCorporationsCorporationIdStructuresServiceStateEnum = typeof GetCorporationsCorporationIdStructuresServiceStateEnum[keyof typeof GetCorporationsCorporationIdStructuresServiceStateEnum];


/**
 * Check if a given object implements the GetCorporationsCorporationIdStructuresService interface.
 */
export function instanceOfGetCorporationsCorporationIdStructuresService(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "name" in value;
    isInstance = isInstance && "state" in value;

    return isInstance;
}

export function GetCorporationsCorporationIdStructuresServiceFromJSON(json: any): GetCorporationsCorporationIdStructuresService {
    return GetCorporationsCorporationIdStructuresServiceFromJSONTyped(json, false);
}

export function GetCorporationsCorporationIdStructuresServiceFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCorporationsCorporationIdStructuresService {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'name': json['name'],
        'state': json['state'],
    };
}

export function GetCorporationsCorporationIdStructuresServiceToJSON(value?: GetCorporationsCorporationIdStructuresService | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'name': value.name,
        'state': value.state,
    };
}

