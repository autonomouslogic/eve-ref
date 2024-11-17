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
 * item object
 * @export
 * @interface GetCharactersCharacterIdFittingsItem
 */
export interface GetCharactersCharacterIdFittingsItem {
    /**
     * flag string
     * @type {string}
     * @memberof GetCharactersCharacterIdFittingsItem
     */
    flag: GetCharactersCharacterIdFittingsItemFlagEnum;
    /**
     * quantity integer
     * @type {number}
     * @memberof GetCharactersCharacterIdFittingsItem
     */
    quantity: number;
    /**
     * type_id integer
     * @type {number}
     * @memberof GetCharactersCharacterIdFittingsItem
     */
    typeId: number;
}


/**
 * @export
 */
export const GetCharactersCharacterIdFittingsItemFlagEnum = {
    Cargo: 'Cargo',
    DroneBay: 'DroneBay',
    FighterBay: 'FighterBay',
    HiSlot0: 'HiSlot0',
    HiSlot1: 'HiSlot1',
    HiSlot2: 'HiSlot2',
    HiSlot3: 'HiSlot3',
    HiSlot4: 'HiSlot4',
    HiSlot5: 'HiSlot5',
    HiSlot6: 'HiSlot6',
    HiSlot7: 'HiSlot7',
    Invalid: 'Invalid',
    LoSlot0: 'LoSlot0',
    LoSlot1: 'LoSlot1',
    LoSlot2: 'LoSlot2',
    LoSlot3: 'LoSlot3',
    LoSlot4: 'LoSlot4',
    LoSlot5: 'LoSlot5',
    LoSlot6: 'LoSlot6',
    LoSlot7: 'LoSlot7',
    MedSlot0: 'MedSlot0',
    MedSlot1: 'MedSlot1',
    MedSlot2: 'MedSlot2',
    MedSlot3: 'MedSlot3',
    MedSlot4: 'MedSlot4',
    MedSlot5: 'MedSlot5',
    MedSlot6: 'MedSlot6',
    MedSlot7: 'MedSlot7',
    RigSlot0: 'RigSlot0',
    RigSlot1: 'RigSlot1',
    RigSlot2: 'RigSlot2',
    ServiceSlot0: 'ServiceSlot0',
    ServiceSlot1: 'ServiceSlot1',
    ServiceSlot2: 'ServiceSlot2',
    ServiceSlot3: 'ServiceSlot3',
    ServiceSlot4: 'ServiceSlot4',
    ServiceSlot5: 'ServiceSlot5',
    ServiceSlot6: 'ServiceSlot6',
    ServiceSlot7: 'ServiceSlot7',
    SubSystemSlot0: 'SubSystemSlot0',
    SubSystemSlot1: 'SubSystemSlot1',
    SubSystemSlot2: 'SubSystemSlot2',
    SubSystemSlot3: 'SubSystemSlot3'
} as const;
export type GetCharactersCharacterIdFittingsItemFlagEnum = typeof GetCharactersCharacterIdFittingsItemFlagEnum[keyof typeof GetCharactersCharacterIdFittingsItemFlagEnum];


/**
 * Check if a given object implements the GetCharactersCharacterIdFittingsItem interface.
 */
export function instanceOfGetCharactersCharacterIdFittingsItem(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "flag" in value;
    isInstance = isInstance && "quantity" in value;
    isInstance = isInstance && "typeId" in value;

    return isInstance;
}

export function GetCharactersCharacterIdFittingsItemFromJSON(json: any): GetCharactersCharacterIdFittingsItem {
    return GetCharactersCharacterIdFittingsItemFromJSONTyped(json, false);
}

export function GetCharactersCharacterIdFittingsItemFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCharactersCharacterIdFittingsItem {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'flag': json['flag'],
        'quantity': json['quantity'],
        'typeId': json['type_id'],
    };
}

export function GetCharactersCharacterIdFittingsItemToJSON(value?: GetCharactersCharacterIdFittingsItem | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'flag': value.flag,
        'quantity': value.quantity,
        'type_id': value.typeId,
    };
}

