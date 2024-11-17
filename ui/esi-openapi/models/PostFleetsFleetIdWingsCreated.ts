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
 * 201 created object
 * @export
 * @interface PostFleetsFleetIdWingsCreated
 */
export interface PostFleetsFleetIdWingsCreated {
    /**
     * The wing_id of the newly created wing
     * @type {number}
     * @memberof PostFleetsFleetIdWingsCreated
     */
    wingId: number;
}

/**
 * Check if a given object implements the PostFleetsFleetIdWingsCreated interface.
 */
export function instanceOfPostFleetsFleetIdWingsCreated(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "wingId" in value;

    return isInstance;
}

export function PostFleetsFleetIdWingsCreatedFromJSON(json: any): PostFleetsFleetIdWingsCreated {
    return PostFleetsFleetIdWingsCreatedFromJSONTyped(json, false);
}

export function PostFleetsFleetIdWingsCreatedFromJSONTyped(json: any, ignoreDiscriminator: boolean): PostFleetsFleetIdWingsCreated {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'wingId': json['wing_id'],
    };
}

export function PostFleetsFleetIdWingsCreatedToJSON(value?: PostFleetsFleetIdWingsCreated | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'wing_id': value.wingId,
    };
}

