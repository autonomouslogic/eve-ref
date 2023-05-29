/**
 * EVE Ref Reference Data for EVE Online
 * This spec should be considered unstable and subject to change at any time.
 *
 * OpenAPI spec version: dev
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { HttpFile } from '../http/http';

export class TraitBonus {
    'bonus'?: number;
    /**
    * The key is the language code.
    */
    'bonus_text'?: { [key: string]: string; };
    'importance'?: number;
    'is_positive'?: boolean;
    'unit_id'?: number;

    static readonly discriminator: string | undefined = undefined;

    static readonly attributeTypeMap: Array<{name: string, baseName: string, type: string, format: string}> = [
        {
            "name": "bonus",
            "baseName": "bonus",
            "type": "number",
            "format": "double"
        },
        {
            "name": "bonus_text",
            "baseName": "bonus_text",
            "type": "{ [key: string]: string; }",
            "format": ""
        },
        {
            "name": "importance",
            "baseName": "importance",
            "type": "number",
            "format": "int32"
        },
        {
            "name": "is_positive",
            "baseName": "is_positive",
            "type": "boolean",
            "format": ""
        },
        {
            "name": "unit_id",
            "baseName": "unit_id",
            "type": "number",
            "format": "int32"
        }    ];

    static getAttributeTypeMap() {
        return TraitBonus.attributeTypeMap;
    }

    public constructor() {
    }
}

