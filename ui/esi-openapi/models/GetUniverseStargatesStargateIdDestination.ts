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
 * destination object
 * @export
 * @interface GetUniverseStargatesStargateIdDestination
 */
export interface GetUniverseStargatesStargateIdDestination {
    /**
     * The stargate this stargate connects to
     * @type {number}
     * @memberof GetUniverseStargatesStargateIdDestination
     */
    stargateId: number;
    /**
     * The solar system this stargate connects to
     * @type {number}
     * @memberof GetUniverseStargatesStargateIdDestination
     */
    systemId: number;
}

/**
 * Check if a given object implements the GetUniverseStargatesStargateIdDestination interface.
 */
export function instanceOfGetUniverseStargatesStargateIdDestination(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "stargateId" in value;
    isInstance = isInstance && "systemId" in value;

    return isInstance;
}

export function GetUniverseStargatesStargateIdDestinationFromJSON(json: any): GetUniverseStargatesStargateIdDestination {
    return GetUniverseStargatesStargateIdDestinationFromJSONTyped(json, false);
}

export function GetUniverseStargatesStargateIdDestinationFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetUniverseStargatesStargateIdDestination {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'stargateId': json['stargate_id'],
        'systemId': json['system_id'],
    };
}

export function GetUniverseStargatesStargateIdDestinationToJSON(value?: GetUniverseStargatesStargateIdDestination | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'stargate_id': value.stargateId,
        'system_id': value.systemId,
    };
}

