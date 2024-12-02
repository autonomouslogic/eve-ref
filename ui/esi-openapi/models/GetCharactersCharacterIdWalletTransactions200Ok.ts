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
 * wallet transaction
 * @export
 * @interface GetCharactersCharacterIdWalletTransactions200Ok
 */
export interface GetCharactersCharacterIdWalletTransactions200Ok {
    /**
     * client_id integer
     * @type {number}
     * @memberof GetCharactersCharacterIdWalletTransactions200Ok
     */
    clientId: number;
    /**
     * Date and time of transaction
     * @type {Date}
     * @memberof GetCharactersCharacterIdWalletTransactions200Ok
     */
    date: Date;
    /**
     * is_buy boolean
     * @type {boolean}
     * @memberof GetCharactersCharacterIdWalletTransactions200Ok
     */
    isBuy: boolean;
    /**
     * is_personal boolean
     * @type {boolean}
     * @memberof GetCharactersCharacterIdWalletTransactions200Ok
     */
    isPersonal: boolean;
    /**
     * journal_ref_id integer
     * @type {number}
     * @memberof GetCharactersCharacterIdWalletTransactions200Ok
     */
    journalRefId: number;
    /**
     * location_id integer
     * @type {number}
     * @memberof GetCharactersCharacterIdWalletTransactions200Ok
     */
    locationId: number;
    /**
     * quantity integer
     * @type {number}
     * @memberof GetCharactersCharacterIdWalletTransactions200Ok
     */
    quantity: number;
    /**
     * Unique transaction ID
     * @type {number}
     * @memberof GetCharactersCharacterIdWalletTransactions200Ok
     */
    transactionId: number;
    /**
     * type_id integer
     * @type {number}
     * @memberof GetCharactersCharacterIdWalletTransactions200Ok
     */
    typeId: number;
    /**
     * Amount paid per unit
     * @type {number}
     * @memberof GetCharactersCharacterIdWalletTransactions200Ok
     */
    unitPrice: number;
}

/**
 * Check if a given object implements the GetCharactersCharacterIdWalletTransactions200Ok interface.
 */
export function instanceOfGetCharactersCharacterIdWalletTransactions200Ok(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "clientId" in value;
    isInstance = isInstance && "date" in value;
    isInstance = isInstance && "isBuy" in value;
    isInstance = isInstance && "isPersonal" in value;
    isInstance = isInstance && "journalRefId" in value;
    isInstance = isInstance && "locationId" in value;
    isInstance = isInstance && "quantity" in value;
    isInstance = isInstance && "transactionId" in value;
    isInstance = isInstance && "typeId" in value;
    isInstance = isInstance && "unitPrice" in value;

    return isInstance;
}

export function GetCharactersCharacterIdWalletTransactions200OkFromJSON(json: any): GetCharactersCharacterIdWalletTransactions200Ok {
    return GetCharactersCharacterIdWalletTransactions200OkFromJSONTyped(json, false);
}

export function GetCharactersCharacterIdWalletTransactions200OkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCharactersCharacterIdWalletTransactions200Ok {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'clientId': json['client_id'],
        'date': (new Date(json['date'])),
        'isBuy': json['is_buy'],
        'isPersonal': json['is_personal'],
        'journalRefId': json['journal_ref_id'],
        'locationId': json['location_id'],
        'quantity': json['quantity'],
        'transactionId': json['transaction_id'],
        'typeId': json['type_id'],
        'unitPrice': json['unit_price'],
    };
}

export function GetCharactersCharacterIdWalletTransactions200OkToJSON(value?: GetCharactersCharacterIdWalletTransactions200Ok | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'client_id': value.clientId,
        'date': (value.date.toISOString()),
        'is_buy': value.isBuy,
        'is_personal': value.isPersonal,
        'journal_ref_id': value.journalRefId,
        'location_id': value.locationId,
        'quantity': value.quantity,
        'transaction_id': value.transactionId,
        'type_id': value.typeId,
        'unit_price': value.unitPrice,
    };
}
