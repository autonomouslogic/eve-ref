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
import type { GetCharactersCharacterIdPlanetsPlanetIdLink } from './GetCharactersCharacterIdPlanetsPlanetIdLink';
import {
    GetCharactersCharacterIdPlanetsPlanetIdLinkFromJSON,
    GetCharactersCharacterIdPlanetsPlanetIdLinkFromJSONTyped,
    GetCharactersCharacterIdPlanetsPlanetIdLinkToJSON,
} from './GetCharactersCharacterIdPlanetsPlanetIdLink';
import type { GetCharactersCharacterIdPlanetsPlanetIdPin } from './GetCharactersCharacterIdPlanetsPlanetIdPin';
import {
    GetCharactersCharacterIdPlanetsPlanetIdPinFromJSON,
    GetCharactersCharacterIdPlanetsPlanetIdPinFromJSONTyped,
    GetCharactersCharacterIdPlanetsPlanetIdPinToJSON,
} from './GetCharactersCharacterIdPlanetsPlanetIdPin';
import type { GetCharactersCharacterIdPlanetsPlanetIdRoute } from './GetCharactersCharacterIdPlanetsPlanetIdRoute';
import {
    GetCharactersCharacterIdPlanetsPlanetIdRouteFromJSON,
    GetCharactersCharacterIdPlanetsPlanetIdRouteFromJSONTyped,
    GetCharactersCharacterIdPlanetsPlanetIdRouteToJSON,
} from './GetCharactersCharacterIdPlanetsPlanetIdRoute';

/**
 * 200 ok object
 * @export
 * @interface GetCharactersCharacterIdPlanetsPlanetIdOk
 */
export interface GetCharactersCharacterIdPlanetsPlanetIdOk {
    /**
     * links array
     * @type {Array<GetCharactersCharacterIdPlanetsPlanetIdLink>}
     * @memberof GetCharactersCharacterIdPlanetsPlanetIdOk
     */
    links: Array<GetCharactersCharacterIdPlanetsPlanetIdLink>;
    /**
     * pins array
     * @type {Array<GetCharactersCharacterIdPlanetsPlanetIdPin>}
     * @memberof GetCharactersCharacterIdPlanetsPlanetIdOk
     */
    pins: Array<GetCharactersCharacterIdPlanetsPlanetIdPin>;
    /**
     * routes array
     * @type {Array<GetCharactersCharacterIdPlanetsPlanetIdRoute>}
     * @memberof GetCharactersCharacterIdPlanetsPlanetIdOk
     */
    routes: Array<GetCharactersCharacterIdPlanetsPlanetIdRoute>;
}

/**
 * Check if a given object implements the GetCharactersCharacterIdPlanetsPlanetIdOk interface.
 */
export function instanceOfGetCharactersCharacterIdPlanetsPlanetIdOk(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "links" in value;
    isInstance = isInstance && "pins" in value;
    isInstance = isInstance && "routes" in value;

    return isInstance;
}

export function GetCharactersCharacterIdPlanetsPlanetIdOkFromJSON(json: any): GetCharactersCharacterIdPlanetsPlanetIdOk {
    return GetCharactersCharacterIdPlanetsPlanetIdOkFromJSONTyped(json, false);
}

export function GetCharactersCharacterIdPlanetsPlanetIdOkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCharactersCharacterIdPlanetsPlanetIdOk {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'links': ((json['links'] as Array<any>).map(GetCharactersCharacterIdPlanetsPlanetIdLinkFromJSON)),
        'pins': ((json['pins'] as Array<any>).map(GetCharactersCharacterIdPlanetsPlanetIdPinFromJSON)),
        'routes': ((json['routes'] as Array<any>).map(GetCharactersCharacterIdPlanetsPlanetIdRouteFromJSON)),
    };
}

export function GetCharactersCharacterIdPlanetsPlanetIdOkToJSON(value?: GetCharactersCharacterIdPlanetsPlanetIdOk | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'links': ((value.links as Array<any>).map(GetCharactersCharacterIdPlanetsPlanetIdLinkToJSON)),
        'pins': ((value.pins as Array<any>).map(GetCharactersCharacterIdPlanetsPlanetIdPinToJSON)),
        'routes': ((value.routes as Array<any>).map(GetCharactersCharacterIdPlanetsPlanetIdRouteToJSON)),
    };
}

