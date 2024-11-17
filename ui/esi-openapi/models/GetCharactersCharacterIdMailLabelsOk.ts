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
import type { GetCharactersCharacterIdMailLabelsLabel } from './GetCharactersCharacterIdMailLabelsLabel';
import {
    GetCharactersCharacterIdMailLabelsLabelFromJSON,
    GetCharactersCharacterIdMailLabelsLabelFromJSONTyped,
    GetCharactersCharacterIdMailLabelsLabelToJSON,
} from './GetCharactersCharacterIdMailLabelsLabel';

/**
 * 200 ok object
 * @export
 * @interface GetCharactersCharacterIdMailLabelsOk
 */
export interface GetCharactersCharacterIdMailLabelsOk {
    /**
     * labels array
     * @type {Array<GetCharactersCharacterIdMailLabelsLabel>}
     * @memberof GetCharactersCharacterIdMailLabelsOk
     */
    labels?: Array<GetCharactersCharacterIdMailLabelsLabel>;
    /**
     * total_unread_count integer
     * @type {number}
     * @memberof GetCharactersCharacterIdMailLabelsOk
     */
    totalUnreadCount?: number;
}

/**
 * Check if a given object implements the GetCharactersCharacterIdMailLabelsOk interface.
 */
export function instanceOfGetCharactersCharacterIdMailLabelsOk(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetCharactersCharacterIdMailLabelsOkFromJSON(json: any): GetCharactersCharacterIdMailLabelsOk {
    return GetCharactersCharacterIdMailLabelsOkFromJSONTyped(json, false);
}

export function GetCharactersCharacterIdMailLabelsOkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCharactersCharacterIdMailLabelsOk {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'labels': !exists(json, 'labels') ? undefined : ((json['labels'] as Array<any>).map(GetCharactersCharacterIdMailLabelsLabelFromJSON)),
        'totalUnreadCount': !exists(json, 'total_unread_count') ? undefined : json['total_unread_count'],
    };
}

export function GetCharactersCharacterIdMailLabelsOkToJSON(value?: GetCharactersCharacterIdMailLabelsOk | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'labels': value.labels === undefined ? undefined : ((value.labels as Array<any>).map(GetCharactersCharacterIdMailLabelsLabelToJSON)),
        'total_unread_count': value.totalUnreadCount,
    };
}

