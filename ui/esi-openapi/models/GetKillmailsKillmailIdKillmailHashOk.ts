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
import type { GetKillmailsKillmailIdKillmailHashAttacker } from './GetKillmailsKillmailIdKillmailHashAttacker';
import {
    GetKillmailsKillmailIdKillmailHashAttackerFromJSON,
    GetKillmailsKillmailIdKillmailHashAttackerFromJSONTyped,
    GetKillmailsKillmailIdKillmailHashAttackerToJSON,
} from './GetKillmailsKillmailIdKillmailHashAttacker';
import type { GetKillmailsKillmailIdKillmailHashVictim } from './GetKillmailsKillmailIdKillmailHashVictim';
import {
    GetKillmailsKillmailIdKillmailHashVictimFromJSON,
    GetKillmailsKillmailIdKillmailHashVictimFromJSONTyped,
    GetKillmailsKillmailIdKillmailHashVictimToJSON,
} from './GetKillmailsKillmailIdKillmailHashVictim';

/**
 * 200 ok object
 * @export
 * @interface GetKillmailsKillmailIdKillmailHashOk
 */
export interface GetKillmailsKillmailIdKillmailHashOk {
    /**
     * attackers array
     * @type {Array<GetKillmailsKillmailIdKillmailHashAttacker>}
     * @memberof GetKillmailsKillmailIdKillmailHashOk
     */
    attackers: Array<GetKillmailsKillmailIdKillmailHashAttacker>;
    /**
     * ID of the killmail
     * @type {number}
     * @memberof GetKillmailsKillmailIdKillmailHashOk
     */
    killmailId: number;
    /**
     * Time that the victim was killed and the killmail generated
     * @type {Date}
     * @memberof GetKillmailsKillmailIdKillmailHashOk
     */
    killmailTime: Date;
    /**
     * Moon if the kill took place at one
     * @type {number}
     * @memberof GetKillmailsKillmailIdKillmailHashOk
     */
    moonId?: number;
    /**
     * Solar system that the kill took place in
     * @type {number}
     * @memberof GetKillmailsKillmailIdKillmailHashOk
     */
    solarSystemId: number;
    /**
     * 
     * @type {GetKillmailsKillmailIdKillmailHashVictim}
     * @memberof GetKillmailsKillmailIdKillmailHashOk
     */
    victim: GetKillmailsKillmailIdKillmailHashVictim;
    /**
     * War if the killmail is generated in relation to an official war
     * @type {number}
     * @memberof GetKillmailsKillmailIdKillmailHashOk
     */
    warId?: number;
}

/**
 * Check if a given object implements the GetKillmailsKillmailIdKillmailHashOk interface.
 */
export function instanceOfGetKillmailsKillmailIdKillmailHashOk(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "attackers" in value;
    isInstance = isInstance && "killmailId" in value;
    isInstance = isInstance && "killmailTime" in value;
    isInstance = isInstance && "solarSystemId" in value;
    isInstance = isInstance && "victim" in value;

    return isInstance;
}

export function GetKillmailsKillmailIdKillmailHashOkFromJSON(json: any): GetKillmailsKillmailIdKillmailHashOk {
    return GetKillmailsKillmailIdKillmailHashOkFromJSONTyped(json, false);
}

export function GetKillmailsKillmailIdKillmailHashOkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetKillmailsKillmailIdKillmailHashOk {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'attackers': ((json['attackers'] as Array<any>).map(GetKillmailsKillmailIdKillmailHashAttackerFromJSON)),
        'killmailId': json['killmail_id'],
        'killmailTime': (new Date(json['killmail_time'])),
        'moonId': !exists(json, 'moon_id') ? undefined : json['moon_id'],
        'solarSystemId': json['solar_system_id'],
        'victim': GetKillmailsKillmailIdKillmailHashVictimFromJSON(json['victim']),
        'warId': !exists(json, 'war_id') ? undefined : json['war_id'],
    };
}

export function GetKillmailsKillmailIdKillmailHashOkToJSON(value?: GetKillmailsKillmailIdKillmailHashOk | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'attackers': ((value.attackers as Array<any>).map(GetKillmailsKillmailIdKillmailHashAttackerToJSON)),
        'killmail_id': value.killmailId,
        'killmail_time': (value.killmailTime.toISOString()),
        'moon_id': value.moonId,
        'solar_system_id': value.solarSystemId,
        'victim': GetKillmailsKillmailIdKillmailHashVictimToJSON(value.victim),
        'war_id': value.warId,
    };
}

