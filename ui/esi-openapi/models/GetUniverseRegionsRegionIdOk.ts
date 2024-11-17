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
 * @interface GetUniverseRegionsRegionIdOk
 */
export interface GetUniverseRegionsRegionIdOk {
    /**
     * constellations array
     * @type {Array<number>}
     * @memberof GetUniverseRegionsRegionIdOk
     */
    constellations: Array<number>;
    /**
     * description string
     * @type {string}
     * @memberof GetUniverseRegionsRegionIdOk
     */
    description?: string;
    /**
     * name string
     * @type {string}
     * @memberof GetUniverseRegionsRegionIdOk
     */
    name: string;
    /**
     * region_id integer
     * @type {number}
     * @memberof GetUniverseRegionsRegionIdOk
     */
    regionId: number;
}

/**
 * Check if a given object implements the GetUniverseRegionsRegionIdOk interface.
 */
export function instanceOfGetUniverseRegionsRegionIdOk(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "constellations" in value;
    isInstance = isInstance && "name" in value;
    isInstance = isInstance && "regionId" in value;

    return isInstance;
}

export function GetUniverseRegionsRegionIdOkFromJSON(json: any): GetUniverseRegionsRegionIdOk {
    return GetUniverseRegionsRegionIdOkFromJSONTyped(json, false);
}

export function GetUniverseRegionsRegionIdOkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetUniverseRegionsRegionIdOk {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'constellations': json['constellations'],
        'description': !exists(json, 'description') ? undefined : json['description'],
        'name': json['name'],
        'regionId': json['region_id'],
    };
}

export function GetUniverseRegionsRegionIdOkToJSON(value?: GetUniverseRegionsRegionIdOk | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'constellations': value.constellations,
        'description': value.description,
        'name': value.name,
        'region_id': value.regionId,
    };
}

