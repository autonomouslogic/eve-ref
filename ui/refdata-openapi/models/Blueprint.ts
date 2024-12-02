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
import type { BlueprintActivity } from './BlueprintActivity';
import {
    BlueprintActivityFromJSON,
    BlueprintActivityFromJSONTyped,
    BlueprintActivityToJSON,
} from './BlueprintActivity';

/**
 * A map of blueprints. The key is the blueprint type ID.
 * @export
 * @interface Blueprint
 */
export interface Blueprint {
    /**
     * 
     * @type {number}
     * @memberof Blueprint
     */
    blueprintTypeId?: number;
    /**
     * 
     * @type {{ [key: string]: BlueprintActivity; }}
     * @memberof Blueprint
     */
    activities?: { [key: string]: BlueprintActivity; };
    /**
     * 
     * @type {number}
     * @memberof Blueprint
     */
    maxProductionLimit?: number;
}

/**
 * Check if a given object implements the Blueprint interface.
 */
export function instanceOfBlueprint(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function BlueprintFromJSON(json: any): Blueprint {
    return BlueprintFromJSONTyped(json, false);
}

export function BlueprintFromJSONTyped(json: any, ignoreDiscriminator: boolean): Blueprint {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'blueprintTypeId': !exists(json, 'blueprint_type_id') ? undefined : json['blueprint_type_id'],
        'activities': !exists(json, 'activities') ? undefined : (mapValues(json['activities'], BlueprintActivityFromJSON)),
        'maxProductionLimit': !exists(json, 'max_production_limit') ? undefined : json['max_production_limit'],
    };
}

export function BlueprintToJSON(value?: Blueprint | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'blueprint_type_id': value.blueprintTypeId,
        'activities': value.activities === undefined ? undefined : (mapValues(value.activities, BlueprintActivityToJSON)),
        'max_production_limit': value.maxProductionLimit,
    };
}
