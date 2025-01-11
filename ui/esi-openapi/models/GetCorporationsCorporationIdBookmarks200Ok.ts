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
import type { GetCorporationsCorporationIdBookmarksCoordinates } from './GetCorporationsCorporationIdBookmarksCoordinates';
import {
    GetCorporationsCorporationIdBookmarksCoordinatesFromJSON,
    GetCorporationsCorporationIdBookmarksCoordinatesFromJSONTyped,
    GetCorporationsCorporationIdBookmarksCoordinatesToJSON,
} from './GetCorporationsCorporationIdBookmarksCoordinates';
import type { GetCorporationsCorporationIdBookmarksItem } from './GetCorporationsCorporationIdBookmarksItem';
import {
    GetCorporationsCorporationIdBookmarksItemFromJSON,
    GetCorporationsCorporationIdBookmarksItemFromJSONTyped,
    GetCorporationsCorporationIdBookmarksItemToJSON,
} from './GetCorporationsCorporationIdBookmarksItem';

/**
 * 200 ok object
 * @export
 * @interface GetCorporationsCorporationIdBookmarks200Ok
 */
export interface GetCorporationsCorporationIdBookmarks200Ok {
    /**
     * bookmark_id integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdBookmarks200Ok
     */
    bookmarkId: number;
    /**
     * 
     * @type {GetCorporationsCorporationIdBookmarksCoordinates}
     * @memberof GetCorporationsCorporationIdBookmarks200Ok
     */
    coordinates?: GetCorporationsCorporationIdBookmarksCoordinates;
    /**
     * created string
     * @type {Date}
     * @memberof GetCorporationsCorporationIdBookmarks200Ok
     */
    created: Date;
    /**
     * creator_id integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdBookmarks200Ok
     */
    creatorId: number;
    /**
     * folder_id integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdBookmarks200Ok
     */
    folderId?: number;
    /**
     * 
     * @type {GetCorporationsCorporationIdBookmarksItem}
     * @memberof GetCorporationsCorporationIdBookmarks200Ok
     */
    item?: GetCorporationsCorporationIdBookmarksItem;
    /**
     * label string
     * @type {string}
     * @memberof GetCorporationsCorporationIdBookmarks200Ok
     */
    label: string;
    /**
     * location_id integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdBookmarks200Ok
     */
    locationId: number;
    /**
     * notes string
     * @type {string}
     * @memberof GetCorporationsCorporationIdBookmarks200Ok
     */
    notes: string;
}

/**
 * Check if a given object implements the GetCorporationsCorporationIdBookmarks200Ok interface.
 */
export function instanceOfGetCorporationsCorporationIdBookmarks200Ok(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "bookmarkId" in value;
    isInstance = isInstance && "created" in value;
    isInstance = isInstance && "creatorId" in value;
    isInstance = isInstance && "label" in value;
    isInstance = isInstance && "locationId" in value;
    isInstance = isInstance && "notes" in value;

    return isInstance;
}

export function GetCorporationsCorporationIdBookmarks200OkFromJSON(json: any): GetCorporationsCorporationIdBookmarks200Ok {
    return GetCorporationsCorporationIdBookmarks200OkFromJSONTyped(json, false);
}

export function GetCorporationsCorporationIdBookmarks200OkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCorporationsCorporationIdBookmarks200Ok {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'bookmarkId': json['bookmark_id'],
        'coordinates': !exists(json, 'coordinates') ? undefined : GetCorporationsCorporationIdBookmarksCoordinatesFromJSON(json['coordinates']),
        'created': (new Date(json['created'])),
        'creatorId': json['creator_id'],
        'folderId': !exists(json, 'folder_id') ? undefined : json['folder_id'],
        'item': !exists(json, 'item') ? undefined : GetCorporationsCorporationIdBookmarksItemFromJSON(json['item']),
        'label': json['label'],
        'locationId': json['location_id'],
        'notes': json['notes'],
    };
}

export function GetCorporationsCorporationIdBookmarks200OkToJSON(value?: GetCorporationsCorporationIdBookmarks200Ok | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'bookmark_id': value.bookmarkId,
        'coordinates': GetCorporationsCorporationIdBookmarksCoordinatesToJSON(value.coordinates),
        'created': (value.created.toISOString()),
        'creator_id': value.creatorId,
        'folder_id': value.folderId,
        'item': GetCorporationsCorporationIdBookmarksItemToJSON(value.item),
        'label': value.label,
        'location_id': value.locationId,
        'notes': value.notes,
    };
}

