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
 * @interface GetCharactersCharacterIdBookmarksFolders200Ok
 */
export interface GetCharactersCharacterIdBookmarksFolders200Ok {
    /**
     * folder_id integer
     * @type {number}
     * @memberof GetCharactersCharacterIdBookmarksFolders200Ok
     */
    folderId: number;
    /**
     * name string
     * @type {string}
     * @memberof GetCharactersCharacterIdBookmarksFolders200Ok
     */
    name: string;
}

/**
 * Check if a given object implements the GetCharactersCharacterIdBookmarksFolders200Ok interface.
 */
export function instanceOfGetCharactersCharacterIdBookmarksFolders200Ok(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "folderId" in value;
    isInstance = isInstance && "name" in value;

    return isInstance;
}

export function GetCharactersCharacterIdBookmarksFolders200OkFromJSON(json: any): GetCharactersCharacterIdBookmarksFolders200Ok {
    return GetCharactersCharacterIdBookmarksFolders200OkFromJSONTyped(json, false);
}

export function GetCharactersCharacterIdBookmarksFolders200OkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCharactersCharacterIdBookmarksFolders200Ok {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'folderId': json['folder_id'],
        'name': json['name'],
    };
}

export function GetCharactersCharacterIdBookmarksFolders200OkToJSON(value?: GetCharactersCharacterIdBookmarksFolders200Ok | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'folder_id': value.folderId,
        'name': value.name,
    };
}
