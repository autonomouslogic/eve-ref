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
import type { Coordinate } from './Coordinate';
import {
    CoordinateFromJSON,
    CoordinateFromJSONTyped,
    CoordinateToJSON,
} from './Coordinate';

/**
 * 
 * @export
 * @interface Region
 */
export interface Region {
    /**
     * 
     * @type {number}
     * @memberof Region
     */
    regionId?: number;
    /**
     * 
     * @type {string}
     * @memberof Region
     */
    universeId?: string;
    /**
     * 
     * @type {number}
     * @memberof Region
     */
    wormholeClassId?: number;
    /**
     * 
     * @type {number}
     * @memberof Region
     */
    nebulaId?: number;
    /**
     * 
     * @type {number}
     * @memberof Region
     */
    nameId?: number;
    /**
     * 
     * @type {number}
     * @memberof Region
     */
    descriptionId?: number;
    /**
     * 
     * @type {number}
     * @memberof Region
     */
    factionId?: number;
    /**
     * The key is the language code.
     * @type {{ [key: string]: string; }}
     * @memberof Region
     */
    name?: { [key: string]: string; };
    /**
     * The key is the language code.
     * @type {{ [key: string]: string; }}
     * @memberof Region
     */
    description?: { [key: string]: string; };
    /**
     * 
     * @type {Coordinate}
     * @memberof Region
     */
    center?: Coordinate;
    /**
     * 
     * @type {Coordinate}
     * @memberof Region
     */
    max?: Coordinate;
    /**
     * 
     * @type {Coordinate}
     * @memberof Region
     */
    min?: Coordinate;
}

/**
 * Check if a given object implements the Region interface.
 */
export function instanceOfRegion(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function RegionFromJSON(json: any): Region {
    return RegionFromJSONTyped(json, false);
}

export function RegionFromJSONTyped(json: any, ignoreDiscriminator: boolean): Region {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'regionId': !exists(json, 'region_id') ? undefined : json['region_id'],
        'universeId': !exists(json, 'universe_id') ? undefined : json['universe_id'],
        'wormholeClassId': !exists(json, 'wormhole_class_id') ? undefined : json['wormhole_class_id'],
        'nebulaId': !exists(json, 'nebula_id') ? undefined : json['nebula_id'],
        'nameId': !exists(json, 'name_id') ? undefined : json['name_id'],
        'descriptionId': !exists(json, 'description_id') ? undefined : json['description_id'],
        'factionId': !exists(json, 'faction_id') ? undefined : json['faction_id'],
        'name': !exists(json, 'name') ? undefined : json['name'],
        'description': !exists(json, 'description') ? undefined : json['description'],
        'center': !exists(json, 'center') ? undefined : CoordinateFromJSON(json['center']),
        'max': !exists(json, 'max') ? undefined : CoordinateFromJSON(json['max']),
        'min': !exists(json, 'min') ? undefined : CoordinateFromJSON(json['min']),
    };
}

export function RegionToJSON(value?: Region | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'region_id': value.regionId,
        'universe_id': value.universeId,
        'wormhole_class_id': value.wormholeClassId,
        'nebula_id': value.nebulaId,
        'name_id': value.nameId,
        'description_id': value.descriptionId,
        'faction_id': value.factionId,
        'name': value.name,
        'description': value.description,
        'center': CoordinateToJSON(value.center),
        'max': CoordinateToJSON(value.max),
        'min': CoordinateToJSON(value.min),
    };
}

