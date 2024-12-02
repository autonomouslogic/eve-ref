/* tslint:disable */
/* eslint-disable */
/**
 * EVE Ref Reference Data for EVE Online
 * This spec should be considered unstable and subject to change at any time.
 *
 * The version of the OpenAPI document: dev
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { exists, mapValues } from '../runtime';
/**
 * A map of icons. The key is the icon ID.
 * @export
 * @interface Icon
 */
export interface Icon {
    /**
     * 
     * @type {number}
     * @memberof Icon
     */
    iconId?: number;
    /**
     * 
     * @type {string}
     * @memberof Icon
     */
    description?: string;
    /**
     * 
     * @type {string}
     * @memberof Icon
     */
    iconFile?: string;
    /**
     * 
     * @type {boolean}
     * @memberof Icon
     */
    obsolete?: boolean;
}

/**
 * Check if a given object implements the Icon interface.
 */
export function instanceOfIcon(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function IconFromJSON(json: any): Icon {
    return IconFromJSONTyped(json, false);
}

export function IconFromJSONTyped(json: any, ignoreDiscriminator: boolean): Icon {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'iconId': !exists(json, 'icon_id') ? undefined : json['icon_id'],
        'description': !exists(json, 'description') ? undefined : json['description'],
        'iconFile': !exists(json, 'icon_file') ? undefined : json['icon_file'],
        'obsolete': !exists(json, 'obsolete') ? undefined : json['obsolete'],
    };
}

export function IconToJSON(value?: Icon | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'icon_id': value.iconId,
        'description': value.description,
        'icon_file': value.iconFile,
        'obsolete': value.obsolete,
    };
}
