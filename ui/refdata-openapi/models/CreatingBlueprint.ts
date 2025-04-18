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

import {exists} from '../runtime';

/**
 * The blueprints creating this type. The key is the blueprint type ID. This is added by EVE Ref.
 * @export
 * @interface CreatingBlueprint
 */
export interface CreatingBlueprint {
    /**
     *
     * @type {number}
     * @memberof CreatingBlueprint
     */
    blueprintTypeId?: number;
    /**
     *
     * @type {string}
     * @memberof CreatingBlueprint
     */
    blueprintActivity?: string;
}

/**
 * Check if a given object implements the CreatingBlueprint interface.
 */
export function instanceOfCreatingBlueprint(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function CreatingBlueprintFromJSON(json: any): CreatingBlueprint {
    return CreatingBlueprintFromJSONTyped(json, false);
}

export function CreatingBlueprintFromJSONTyped(json: any, ignoreDiscriminator: boolean): CreatingBlueprint {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {

        'blueprintTypeId': !exists(json, 'blueprint_type_id') ? undefined : json['blueprint_type_id'],
        'blueprintActivity': !exists(json, 'blueprint_activity') ? undefined : json['blueprint_activity'],
    };
}

export function CreatingBlueprintToJSON(value?: CreatingBlueprint | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {

        'blueprint_type_id': value.blueprintTypeId,
        'blueprint_activity': value.blueprintActivity,
    };
}

