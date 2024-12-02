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
 * @interface GetCorporationCorporationIdMiningExtractions200Ok
 */
export interface GetCorporationCorporationIdMiningExtractions200Ok {
    /**
     * The time at which the chunk being extracted will arrive and can be fractured by the moon mining drill.
     * @type {Date}
     * @memberof GetCorporationCorporationIdMiningExtractions200Ok
     */
    chunkArrivalTime: Date;
    /**
     * The time at which the current extraction was initiated.
     * @type {Date}
     * @memberof GetCorporationCorporationIdMiningExtractions200Ok
     */
    extractionStartTime: Date;
    /**
     * moon_id integer
     * @type {number}
     * @memberof GetCorporationCorporationIdMiningExtractions200Ok
     */
    moonId: number;
    /**
     * The time at which the chunk being extracted will naturally fracture if it is not first fractured by the moon mining drill.
     * @type {Date}
     * @memberof GetCorporationCorporationIdMiningExtractions200Ok
     */
    naturalDecayTime: Date;
    /**
     * structure_id integer
     * @type {number}
     * @memberof GetCorporationCorporationIdMiningExtractions200Ok
     */
    structureId: number;
}

/**
 * Check if a given object implements the GetCorporationCorporationIdMiningExtractions200Ok interface.
 */
export function instanceOfGetCorporationCorporationIdMiningExtractions200Ok(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "chunkArrivalTime" in value;
    isInstance = isInstance && "extractionStartTime" in value;
    isInstance = isInstance && "moonId" in value;
    isInstance = isInstance && "naturalDecayTime" in value;
    isInstance = isInstance && "structureId" in value;

    return isInstance;
}

export function GetCorporationCorporationIdMiningExtractions200OkFromJSON(json: any): GetCorporationCorporationIdMiningExtractions200Ok {
    return GetCorporationCorporationIdMiningExtractions200OkFromJSONTyped(json, false);
}

export function GetCorporationCorporationIdMiningExtractions200OkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCorporationCorporationIdMiningExtractions200Ok {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'chunkArrivalTime': (new Date(json['chunk_arrival_time'])),
        'extractionStartTime': (new Date(json['extraction_start_time'])),
        'moonId': json['moon_id'],
        'naturalDecayTime': (new Date(json['natural_decay_time'])),
        'structureId': json['structure_id'],
    };
}

export function GetCorporationCorporationIdMiningExtractions200OkToJSON(value?: GetCorporationCorporationIdMiningExtractions200Ok | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'chunk_arrival_time': (value.chunkArrivalTime.toISOString()),
        'extraction_start_time': (value.extractionStartTime.toISOString()),
        'moon_id': value.moonId,
        'natural_decay_time': (value.naturalDecayTime.toISOString()),
        'structure_id': value.structureId,
    };
}
