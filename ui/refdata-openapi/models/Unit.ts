/* tslint:disable */
/* eslint-disable */
/**
 * EVE Ref Reference Data for EVE Online
 * This spec should be considered unstable and subject to change at any time.
 *
 * The version of the OpenAPI document: dev
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { exists, mapValues } from '../runtime';
/**
 * 
 * @export
 * @interface Unit
 */
export interface Unit {
    /**
     * 
     * @type {{ [key: string]: string; }}
     * @memberof Unit
     */
    description?: { [key: string]: string; };
    /**
     * 
     * @type {string}
     * @memberof Unit
     */
    displayName?: string;
    /**
     * 
     * @type {{ [key: string]: string; }}
     * @memberof Unit
     */
    name?: { [key: string]: string; };
    /**
     * 
     * @type {number}
     * @memberof Unit
     */
    unitId?: number;
}

/**
 * Check if a given object implements the Unit interface.
 */
export function instanceOfUnit(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function UnitFromJSON(json: any): Unit {
    return UnitFromJSONTyped(json, false);
}

export function UnitFromJSONTyped(json: any, ignoreDiscriminator: boolean): Unit {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'description': !exists(json, 'description') ? undefined : json['description'],
        'displayName': !exists(json, 'display_name') ? undefined : json['display_name'],
        'name': !exists(json, 'name') ? undefined : json['name'],
        'unitId': !exists(json, 'unit_id') ? undefined : json['unit_id'],
    };
}

export function UnitToJSON(value?: Unit | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'description': value.description,
        'display_name': value.displayName,
        'name': value.name,
        'unit_id': value.unitId,
    };
}

