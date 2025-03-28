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
 * label object
 * @export
 * @interface GetCharactersCharacterIdMailLabelsLabel
 */
export interface GetCharactersCharacterIdMailLabelsLabel {
    /**
     * color string
     * @type {string}
     * @memberof GetCharactersCharacterIdMailLabelsLabel
     */
    color?: GetCharactersCharacterIdMailLabelsLabelColorEnum;
    /**
     * label_id integer
     * @type {number}
     * @memberof GetCharactersCharacterIdMailLabelsLabel
     */
    labelId?: number;
    /**
     * name string
     * @type {string}
     * @memberof GetCharactersCharacterIdMailLabelsLabel
     */
    name?: string;
    /**
     * unread_count integer
     * @type {number}
     * @memberof GetCharactersCharacterIdMailLabelsLabel
     */
    unreadCount?: number;
}


/**
 * @export
 */
export const GetCharactersCharacterIdMailLabelsLabelColorEnum = {
    _0000fe: '#0000fe',
    _006634: '#006634',
    _0099ff: '#0099ff',
    _00ff33: '#00ff33',
    _01ffff: '#01ffff',
    _349800: '#349800',
    _660066: '#660066',
    _666666: '#666666',
    _999999: '#999999',
    _99ffff: '#99ffff',
    _9a0000: '#9a0000',
    Ccff9a: '#ccff9a',
    E6e6e6: '#e6e6e6',
    Fe0000: '#fe0000',
    Ff6600: '#ff6600',
    Ffff01: '#ffff01',
    Ffffcd: '#ffffcd',
    Ffffff: '#ffffff'
} as const;
export type GetCharactersCharacterIdMailLabelsLabelColorEnum = typeof GetCharactersCharacterIdMailLabelsLabelColorEnum[keyof typeof GetCharactersCharacterIdMailLabelsLabelColorEnum];


/**
 * Check if a given object implements the GetCharactersCharacterIdMailLabelsLabel interface.
 */
export function instanceOfGetCharactersCharacterIdMailLabelsLabel(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetCharactersCharacterIdMailLabelsLabelFromJSON(json: any): GetCharactersCharacterIdMailLabelsLabel {
    return GetCharactersCharacterIdMailLabelsLabelFromJSONTyped(json, false);
}

export function GetCharactersCharacterIdMailLabelsLabelFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCharactersCharacterIdMailLabelsLabel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'color': !exists(json, 'color') ? undefined : json['color'],
        'labelId': !exists(json, 'label_id') ? undefined : json['label_id'],
        'name': !exists(json, 'name') ? undefined : json['name'],
        'unreadCount': !exists(json, 'unread_count') ? undefined : json['unread_count'],
    };
}

export function GetCharactersCharacterIdMailLabelsLabelToJSON(value?: GetCharactersCharacterIdMailLabelsLabel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'color': value.color,
        'label_id': value.labelId,
        'name': value.name,
        'unread_count': value.unreadCount,
    };
}

