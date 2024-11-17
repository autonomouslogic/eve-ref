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
import type { GetWarsWarIdAggressor } from './GetWarsWarIdAggressor';
import {
    GetWarsWarIdAggressorFromJSON,
    GetWarsWarIdAggressorFromJSONTyped,
    GetWarsWarIdAggressorToJSON,
} from './GetWarsWarIdAggressor';
import type { GetWarsWarIdAlly } from './GetWarsWarIdAlly';
import {
    GetWarsWarIdAllyFromJSON,
    GetWarsWarIdAllyFromJSONTyped,
    GetWarsWarIdAllyToJSON,
} from './GetWarsWarIdAlly';
import type { GetWarsWarIdDefender } from './GetWarsWarIdDefender';
import {
    GetWarsWarIdDefenderFromJSON,
    GetWarsWarIdDefenderFromJSONTyped,
    GetWarsWarIdDefenderToJSON,
} from './GetWarsWarIdDefender';

/**
 * 200 ok object
 * @export
 * @interface GetWarsWarIdOk
 */
export interface GetWarsWarIdOk {
    /**
     * 
     * @type {GetWarsWarIdAggressor}
     * @memberof GetWarsWarIdOk
     */
    aggressor: GetWarsWarIdAggressor;
    /**
     * allied corporations or alliances, each object contains either corporation_id or alliance_id
     * @type {Array<GetWarsWarIdAlly>}
     * @memberof GetWarsWarIdOk
     */
    allies?: Array<GetWarsWarIdAlly>;
    /**
     * Time that the war was declared
     * @type {Date}
     * @memberof GetWarsWarIdOk
     */
    declared: Date;
    /**
     * 
     * @type {GetWarsWarIdDefender}
     * @memberof GetWarsWarIdOk
     */
    defender: GetWarsWarIdDefender;
    /**
     * Time the war ended and shooting was no longer allowed
     * @type {Date}
     * @memberof GetWarsWarIdOk
     */
    finished?: Date;
    /**
     * ID of the specified war
     * @type {number}
     * @memberof GetWarsWarIdOk
     */
    id: number;
    /**
     * Was the war declared mutual by both parties
     * @type {boolean}
     * @memberof GetWarsWarIdOk
     */
    mutual: boolean;
    /**
     * Is the war currently open for allies or not
     * @type {boolean}
     * @memberof GetWarsWarIdOk
     */
    openForAllies: boolean;
    /**
     * Time the war was retracted but both sides could still shoot each other
     * @type {Date}
     * @memberof GetWarsWarIdOk
     */
    retracted?: Date;
    /**
     * Time when the war started and both sides could shoot each other
     * @type {Date}
     * @memberof GetWarsWarIdOk
     */
    started?: Date;
}

/**
 * Check if a given object implements the GetWarsWarIdOk interface.
 */
export function instanceOfGetWarsWarIdOk(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "aggressor" in value;
    isInstance = isInstance && "declared" in value;
    isInstance = isInstance && "defender" in value;
    isInstance = isInstance && "id" in value;
    isInstance = isInstance && "mutual" in value;
    isInstance = isInstance && "openForAllies" in value;

    return isInstance;
}

export function GetWarsWarIdOkFromJSON(json: any): GetWarsWarIdOk {
    return GetWarsWarIdOkFromJSONTyped(json, false);
}

export function GetWarsWarIdOkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetWarsWarIdOk {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'aggressor': GetWarsWarIdAggressorFromJSON(json['aggressor']),
        'allies': !exists(json, 'allies') ? undefined : ((json['allies'] as Array<any>).map(GetWarsWarIdAllyFromJSON)),
        'declared': (new Date(json['declared'])),
        'defender': GetWarsWarIdDefenderFromJSON(json['defender']),
        'finished': !exists(json, 'finished') ? undefined : (new Date(json['finished'])),
        'id': json['id'],
        'mutual': json['mutual'],
        'openForAllies': json['open_for_allies'],
        'retracted': !exists(json, 'retracted') ? undefined : (new Date(json['retracted'])),
        'started': !exists(json, 'started') ? undefined : (new Date(json['started'])),
    };
}

export function GetWarsWarIdOkToJSON(value?: GetWarsWarIdOk | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'aggressor': GetWarsWarIdAggressorToJSON(value.aggressor),
        'allies': value.allies === undefined ? undefined : ((value.allies as Array<any>).map(GetWarsWarIdAllyToJSON)),
        'declared': (value.declared.toISOString()),
        'defender': GetWarsWarIdDefenderToJSON(value.defender),
        'finished': value.finished === undefined ? undefined : (value.finished.toISOString()),
        'id': value.id,
        'mutual': value.mutual,
        'open_for_allies': value.openForAllies,
        'retracted': value.retracted === undefined ? undefined : (value.retracted.toISOString()),
        'started': value.started === undefined ? undefined : (value.started.toISOString()),
    };
}

