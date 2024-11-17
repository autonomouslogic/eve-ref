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
 * label object
 * @export
 * @interface PostCharactersCharacterIdMailLabelsLabel
 */
export interface PostCharactersCharacterIdMailLabelsLabel {
    /**
     * Hexadecimal string representing label color, in RGB format
     * @type {string}
     * @memberof PostCharactersCharacterIdMailLabelsLabel
     */
    color?: PostCharactersCharacterIdMailLabelsLabelColorEnum;
    /**
     * name string
     * @type {string}
     * @memberof PostCharactersCharacterIdMailLabelsLabel
     */
    name: string;
}


/**
 * @export
 */
export const PostCharactersCharacterIdMailLabelsLabelColorEnum = {
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
export type PostCharactersCharacterIdMailLabelsLabelColorEnum = typeof PostCharactersCharacterIdMailLabelsLabelColorEnum[keyof typeof PostCharactersCharacterIdMailLabelsLabelColorEnum];


/**
 * Check if a given object implements the PostCharactersCharacterIdMailLabelsLabel interface.
 */
export function instanceOfPostCharactersCharacterIdMailLabelsLabel(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "name" in value;

    return isInstance;
}

export function PostCharactersCharacterIdMailLabelsLabelFromJSON(json: any): PostCharactersCharacterIdMailLabelsLabel {
    return PostCharactersCharacterIdMailLabelsLabelFromJSONTyped(json, false);
}

export function PostCharactersCharacterIdMailLabelsLabelFromJSONTyped(json: any, ignoreDiscriminator: boolean): PostCharactersCharacterIdMailLabelsLabel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'color': !exists(json, 'color') ? undefined : json['color'],
        'name': json['name'],
    };
}

export function PostCharactersCharacterIdMailLabelsLabelToJSON(value?: PostCharactersCharacterIdMailLabelsLabel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'color': value.color,
        'name': value.name,
    };
}

