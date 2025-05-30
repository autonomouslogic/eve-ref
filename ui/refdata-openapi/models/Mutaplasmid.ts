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
import type { MutaplasmidDogmaModifications } from './MutaplasmidDogmaModifications';
import {
    MutaplasmidDogmaModificationsFromJSON,
    MutaplasmidDogmaModificationsFromJSONTyped,
    MutaplasmidDogmaModificationsToJSON,
} from './MutaplasmidDogmaModifications';
import type { MutaplasmidTypeMapping } from './MutaplasmidTypeMapping';
import {
    MutaplasmidTypeMappingFromJSON,
    MutaplasmidTypeMappingFromJSONTyped,
    MutaplasmidTypeMappingToJSON,
} from './MutaplasmidTypeMapping';

/**
 * Details about a mutaplasmid. These are created by EVE Ref and derived from Hoboleaks.
 * @export
 * @interface Mutaplasmid
 */
export interface Mutaplasmid {
    /**
     * Which dogma attributes are modified by this mutaplasmid and by how much.
     * @type {{ [key: string]: MutaplasmidDogmaModifications; }}
     * @memberof Mutaplasmid
     */
    dogmaModifications?: { [key: string]: MutaplasmidDogmaModifications; };
    /**
     * 
     * @type {number}
     * @memberof Mutaplasmid
     */
    typeId?: number;
    /**
     * 
     * @type {{ [key: string]: MutaplasmidTypeMapping; }}
     * @memberof Mutaplasmid
     */
    typeMappings?: { [key: string]: MutaplasmidTypeMapping; };
}

/**
 * Check if a given object implements the Mutaplasmid interface.
 */
export function instanceOfMutaplasmid(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function MutaplasmidFromJSON(json: any): Mutaplasmid {
    return MutaplasmidFromJSONTyped(json, false);
}

export function MutaplasmidFromJSONTyped(json: any, ignoreDiscriminator: boolean): Mutaplasmid {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'dogmaModifications': !exists(json, 'dogma_modifications') ? undefined : (mapValues(json['dogma_modifications'], MutaplasmidDogmaModificationsFromJSON)),
        'typeId': !exists(json, 'type_id') ? undefined : json['type_id'],
        'typeMappings': !exists(json, 'type_mappings') ? undefined : (mapValues(json['type_mappings'], MutaplasmidTypeMappingFromJSON)),
    };
}

export function MutaplasmidToJSON(value?: Mutaplasmid | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'dogma_modifications': value.dogmaModifications === undefined ? undefined : (mapValues(value.dogmaModifications, MutaplasmidDogmaModificationsToJSON)),
        'type_id': value.typeId,
        'type_mappings': value.typeMappings === undefined ? undefined : (mapValues(value.typeMappings, MutaplasmidTypeMappingToJSON)),
    };
}

