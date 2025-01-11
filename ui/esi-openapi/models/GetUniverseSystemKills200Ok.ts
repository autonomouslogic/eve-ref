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
 * @interface GetUniverseSystemKills200Ok
 */
export interface GetUniverseSystemKills200Ok {
    /**
     * Number of NPC ships killed in this system
     * @type {number}
     * @memberof GetUniverseSystemKills200Ok
     */
    npcKills: number;
    /**
     * Number of pods killed in this system
     * @type {number}
     * @memberof GetUniverseSystemKills200Ok
     */
    podKills: number;
    /**
     * Number of player ships killed in this system
     * @type {number}
     * @memberof GetUniverseSystemKills200Ok
     */
    shipKills: number;
    /**
     * system_id integer
     * @type {number}
     * @memberof GetUniverseSystemKills200Ok
     */
    systemId: number;
}

/**
 * Check if a given object implements the GetUniverseSystemKills200Ok interface.
 */
export function instanceOfGetUniverseSystemKills200Ok(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "npcKills" in value;
    isInstance = isInstance && "podKills" in value;
    isInstance = isInstance && "shipKills" in value;
    isInstance = isInstance && "systemId" in value;

    return isInstance;
}

export function GetUniverseSystemKills200OkFromJSON(json: any): GetUniverseSystemKills200Ok {
    return GetUniverseSystemKills200OkFromJSONTyped(json, false);
}

export function GetUniverseSystemKills200OkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetUniverseSystemKills200Ok {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'npcKills': json['npc_kills'],
        'podKills': json['pod_kills'],
        'shipKills': json['ship_kills'],
        'systemId': json['system_id'],
    };
}

export function GetUniverseSystemKills200OkToJSON(value?: GetUniverseSystemKills200Ok | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'npc_kills': value.npcKills,
        'pod_kills': value.podKills,
        'ship_kills': value.shipKills,
        'system_id': value.systemId,
    };
}

