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
 * dogma_attribute object
 * @export
 * @interface GetUniverseTypesTypeIdDogmaAttribute
 */
export interface GetUniverseTypesTypeIdDogmaAttribute {
    /**
     * attribute_id integer
     * @type {number}
     * @memberof GetUniverseTypesTypeIdDogmaAttribute
     */
    attributeId: number;
    /**
     * value number
     * @type {number}
     * @memberof GetUniverseTypesTypeIdDogmaAttribute
     */
    value: number;
}

/**
 * Check if a given object implements the GetUniverseTypesTypeIdDogmaAttribute interface.
 */
export function instanceOfGetUniverseTypesTypeIdDogmaAttribute(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "attributeId" in value;
    isInstance = isInstance && "value" in value;

    return isInstance;
}

export function GetUniverseTypesTypeIdDogmaAttributeFromJSON(json: any): GetUniverseTypesTypeIdDogmaAttribute {
    return GetUniverseTypesTypeIdDogmaAttributeFromJSONTyped(json, false);
}

export function GetUniverseTypesTypeIdDogmaAttributeFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetUniverseTypesTypeIdDogmaAttribute {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'attributeId': json['attribute_id'],
        'value': json['value'],
    };
}

export function GetUniverseTypesTypeIdDogmaAttributeToJSON(value?: GetUniverseTypesTypeIdDogmaAttribute | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'attribute_id': value.attributeId,
        'value': value.value,
    };
}

