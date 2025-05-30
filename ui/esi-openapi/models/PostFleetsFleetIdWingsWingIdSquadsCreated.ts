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
 * 201 created object
 * @export
 * @interface PostFleetsFleetIdWingsWingIdSquadsCreated
 */
export interface PostFleetsFleetIdWingsWingIdSquadsCreated {
    /**
     * The squad_id of the newly created squad
     * @type {number}
     * @memberof PostFleetsFleetIdWingsWingIdSquadsCreated
     */
    squadId: number;
}

/**
 * Check if a given object implements the PostFleetsFleetIdWingsWingIdSquadsCreated interface.
 */
export function instanceOfPostFleetsFleetIdWingsWingIdSquadsCreated(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "squadId" in value;

    return isInstance;
}

export function PostFleetsFleetIdWingsWingIdSquadsCreatedFromJSON(json: any): PostFleetsFleetIdWingsWingIdSquadsCreated {
    return PostFleetsFleetIdWingsWingIdSquadsCreatedFromJSONTyped(json, false);
}

export function PostFleetsFleetIdWingsWingIdSquadsCreatedFromJSONTyped(json: any, ignoreDiscriminator: boolean): PostFleetsFleetIdWingsWingIdSquadsCreated {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'squadId': json['squad_id'],
    };
}

export function PostFleetsFleetIdWingsWingIdSquadsCreatedToJSON(value?: PostFleetsFleetIdWingsWingIdSquadsCreated | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'squad_id': value.squadId,
    };
}

