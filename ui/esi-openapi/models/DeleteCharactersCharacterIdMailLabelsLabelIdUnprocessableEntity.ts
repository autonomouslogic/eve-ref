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
 * Unprocessable entity
 * @export
 * @interface DeleteCharactersCharacterIdMailLabelsLabelIdUnprocessableEntity
 */
export interface DeleteCharactersCharacterIdMailLabelsLabelIdUnprocessableEntity {
    /**
     * Unprocessable entity message
     * @type {string}
     * @memberof DeleteCharactersCharacterIdMailLabelsLabelIdUnprocessableEntity
     */
    error?: string;
}

/**
 * Check if a given object implements the DeleteCharactersCharacterIdMailLabelsLabelIdUnprocessableEntity interface.
 */
export function instanceOfDeleteCharactersCharacterIdMailLabelsLabelIdUnprocessableEntity(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function DeleteCharactersCharacterIdMailLabelsLabelIdUnprocessableEntityFromJSON(json: any): DeleteCharactersCharacterIdMailLabelsLabelIdUnprocessableEntity {
    return DeleteCharactersCharacterIdMailLabelsLabelIdUnprocessableEntityFromJSONTyped(json, false);
}

export function DeleteCharactersCharacterIdMailLabelsLabelIdUnprocessableEntityFromJSONTyped(json: any, ignoreDiscriminator: boolean): DeleteCharactersCharacterIdMailLabelsLabelIdUnprocessableEntity {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'error': !exists(json, 'error') ? undefined : json['error'],
    };
}

export function DeleteCharactersCharacterIdMailLabelsLabelIdUnprocessableEntityToJSON(value?: DeleteCharactersCharacterIdMailLabelsLabelIdUnprocessableEntity | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'error': value.error,
    };
}

