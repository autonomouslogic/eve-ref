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
 * 200 ok object
 * @export
 * @interface GetCharactersCharacterIdShipOk
 */
export interface GetCharactersCharacterIdShipOk {
    /**
     * Item id's are unique to a ship and persist until it is repackaged. This value can be used to track repeated uses of a ship, or detect when a pilot changes into a different instance of the same ship type.
     * @type {number}
     * @memberof GetCharactersCharacterIdShipOk
     */
    shipItemId: number;
    /**
     * ship_name string
     * @type {string}
     * @memberof GetCharactersCharacterIdShipOk
     */
    shipName: string;
    /**
     * ship_type_id integer
     * @type {number}
     * @memberof GetCharactersCharacterIdShipOk
     */
    shipTypeId: number;
}

/**
 * Check if a given object implements the GetCharactersCharacterIdShipOk interface.
 */
export function instanceOfGetCharactersCharacterIdShipOk(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "shipItemId" in value;
    isInstance = isInstance && "shipName" in value;
    isInstance = isInstance && "shipTypeId" in value;

    return isInstance;
}

export function GetCharactersCharacterIdShipOkFromJSON(json: any): GetCharactersCharacterIdShipOk {
    return GetCharactersCharacterIdShipOkFromJSONTyped(json, false);
}

export function GetCharactersCharacterIdShipOkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCharactersCharacterIdShipOk {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'shipItemId': json['ship_item_id'],
        'shipName': json['ship_name'],
        'shipTypeId': json['ship_type_id'],
    };
}

export function GetCharactersCharacterIdShipOkToJSON(value?: GetCharactersCharacterIdShipOk | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'ship_item_id': value.shipItemId,
        'ship_name': value.shipName,
        'ship_type_id': value.shipTypeId,
    };
}

