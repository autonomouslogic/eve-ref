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
 * @interface PostCharactersCharacterIdFittingsCreated
 */
export interface PostCharactersCharacterIdFittingsCreated {
    /**
     * fitting_id integer
     * @type {number}
     * @memberof PostCharactersCharacterIdFittingsCreated
     */
    fittingId: number;
}

/**
 * Check if a given object implements the PostCharactersCharacterIdFittingsCreated interface.
 */
export function instanceOfPostCharactersCharacterIdFittingsCreated(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "fittingId" in value;

    return isInstance;
}

export function PostCharactersCharacterIdFittingsCreatedFromJSON(json: any): PostCharactersCharacterIdFittingsCreated {
    return PostCharactersCharacterIdFittingsCreatedFromJSONTyped(json, false);
}

export function PostCharactersCharacterIdFittingsCreatedFromJSONTyped(json: any, ignoreDiscriminator: boolean): PostCharactersCharacterIdFittingsCreated {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'fittingId': json['fitting_id'],
    };
}

export function PostCharactersCharacterIdFittingsCreatedToJSON(value?: PostCharactersCharacterIdFittingsCreated | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'fitting_id': value.fittingId,
    };
}

