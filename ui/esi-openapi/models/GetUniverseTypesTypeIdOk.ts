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
import type { GetUniverseTypesTypeIdDogmaAttribute } from './GetUniverseTypesTypeIdDogmaAttribute';
import {
    GetUniverseTypesTypeIdDogmaAttributeFromJSON,
    GetUniverseTypesTypeIdDogmaAttributeFromJSONTyped,
    GetUniverseTypesTypeIdDogmaAttributeToJSON,
} from './GetUniverseTypesTypeIdDogmaAttribute';
import type { GetUniverseTypesTypeIdDogmaEffect } from './GetUniverseTypesTypeIdDogmaEffect';
import {
    GetUniverseTypesTypeIdDogmaEffectFromJSON,
    GetUniverseTypesTypeIdDogmaEffectFromJSONTyped,
    GetUniverseTypesTypeIdDogmaEffectToJSON,
} from './GetUniverseTypesTypeIdDogmaEffect';

/**
 * 200 ok object
 * @export
 * @interface GetUniverseTypesTypeIdOk
 */
export interface GetUniverseTypesTypeIdOk {
    /**
     * capacity number
     * @type {number}
     * @memberof GetUniverseTypesTypeIdOk
     */
    capacity?: number;
    /**
     * description string
     * @type {string}
     * @memberof GetUniverseTypesTypeIdOk
     */
    description: string;
    /**
     * dogma_attributes array
     * @type {Array<GetUniverseTypesTypeIdDogmaAttribute>}
     * @memberof GetUniverseTypesTypeIdOk
     */
    dogmaAttributes?: Array<GetUniverseTypesTypeIdDogmaAttribute>;
    /**
     * dogma_effects array
     * @type {Array<GetUniverseTypesTypeIdDogmaEffect>}
     * @memberof GetUniverseTypesTypeIdOk
     */
    dogmaEffects?: Array<GetUniverseTypesTypeIdDogmaEffect>;
    /**
     * graphic_id integer
     * @type {number}
     * @memberof GetUniverseTypesTypeIdOk
     */
    graphicId?: number;
    /**
     * group_id integer
     * @type {number}
     * @memberof GetUniverseTypesTypeIdOk
     */
    groupId: number;
    /**
     * icon_id integer
     * @type {number}
     * @memberof GetUniverseTypesTypeIdOk
     */
    iconId?: number;
    /**
     * This only exists for types that can be put on the market
     * @type {number}
     * @memberof GetUniverseTypesTypeIdOk
     */
    marketGroupId?: number;
    /**
     * mass number
     * @type {number}
     * @memberof GetUniverseTypesTypeIdOk
     */
    mass?: number;
    /**
     * name string
     * @type {string}
     * @memberof GetUniverseTypesTypeIdOk
     */
    name: string;
    /**
     * packaged_volume number
     * @type {number}
     * @memberof GetUniverseTypesTypeIdOk
     */
    packagedVolume?: number;
    /**
     * portion_size integer
     * @type {number}
     * @memberof GetUniverseTypesTypeIdOk
     */
    portionSize?: number;
    /**
     * published boolean
     * @type {boolean}
     * @memberof GetUniverseTypesTypeIdOk
     */
    published: boolean;
    /**
     * radius number
     * @type {number}
     * @memberof GetUniverseTypesTypeIdOk
     */
    radius?: number;
    /**
     * type_id integer
     * @type {number}
     * @memberof GetUniverseTypesTypeIdOk
     */
    typeId: number;
    /**
     * volume number
     * @type {number}
     * @memberof GetUniverseTypesTypeIdOk
     */
    volume?: number;
}

/**
 * Check if a given object implements the GetUniverseTypesTypeIdOk interface.
 */
export function instanceOfGetUniverseTypesTypeIdOk(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "description" in value;
    isInstance = isInstance && "groupId" in value;
    isInstance = isInstance && "name" in value;
    isInstance = isInstance && "published" in value;
    isInstance = isInstance && "typeId" in value;

    return isInstance;
}

export function GetUniverseTypesTypeIdOkFromJSON(json: any): GetUniverseTypesTypeIdOk {
    return GetUniverseTypesTypeIdOkFromJSONTyped(json, false);
}

export function GetUniverseTypesTypeIdOkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetUniverseTypesTypeIdOk {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'capacity': !exists(json, 'capacity') ? undefined : json['capacity'],
        'description': json['description'],
        'dogmaAttributes': !exists(json, 'dogma_attributes') ? undefined : ((json['dogma_attributes'] as Array<any>).map(GetUniverseTypesTypeIdDogmaAttributeFromJSON)),
        'dogmaEffects': !exists(json, 'dogma_effects') ? undefined : ((json['dogma_effects'] as Array<any>).map(GetUniverseTypesTypeIdDogmaEffectFromJSON)),
        'graphicId': !exists(json, 'graphic_id') ? undefined : json['graphic_id'],
        'groupId': json['group_id'],
        'iconId': !exists(json, 'icon_id') ? undefined : json['icon_id'],
        'marketGroupId': !exists(json, 'market_group_id') ? undefined : json['market_group_id'],
        'mass': !exists(json, 'mass') ? undefined : json['mass'],
        'name': json['name'],
        'packagedVolume': !exists(json, 'packaged_volume') ? undefined : json['packaged_volume'],
        'portionSize': !exists(json, 'portion_size') ? undefined : json['portion_size'],
        'published': json['published'],
        'radius': !exists(json, 'radius') ? undefined : json['radius'],
        'typeId': json['type_id'],
        'volume': !exists(json, 'volume') ? undefined : json['volume'],
    };
}

export function GetUniverseTypesTypeIdOkToJSON(value?: GetUniverseTypesTypeIdOk | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'capacity': value.capacity,
        'description': value.description,
        'dogma_attributes': value.dogmaAttributes === undefined ? undefined : ((value.dogmaAttributes as Array<any>).map(GetUniverseTypesTypeIdDogmaAttributeToJSON)),
        'dogma_effects': value.dogmaEffects === undefined ? undefined : ((value.dogmaEffects as Array<any>).map(GetUniverseTypesTypeIdDogmaEffectToJSON)),
        'graphic_id': value.graphicId,
        'group_id': value.groupId,
        'icon_id': value.iconId,
        'market_group_id': value.marketGroupId,
        'mass': value.mass,
        'name': value.name,
        'packaged_volume': value.packagedVolume,
        'portion_size': value.portionSize,
        'published': value.published,
        'radius': value.radius,
        'type_id': value.typeId,
        'volume': value.volume,
    };
}

