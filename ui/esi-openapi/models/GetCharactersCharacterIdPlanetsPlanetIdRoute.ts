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
 * route object
 * @export
 * @interface GetCharactersCharacterIdPlanetsPlanetIdRoute
 */
export interface GetCharactersCharacterIdPlanetsPlanetIdRoute {
    /**
     * content_type_id integer
     * @type {number}
     * @memberof GetCharactersCharacterIdPlanetsPlanetIdRoute
     */
    contentTypeId: number;
    /**
     * destination_pin_id integer
     * @type {number}
     * @memberof GetCharactersCharacterIdPlanetsPlanetIdRoute
     */
    destinationPinId: number;
    /**
     * quantity number
     * @type {number}
     * @memberof GetCharactersCharacterIdPlanetsPlanetIdRoute
     */
    quantity: number;
    /**
     * route_id integer
     * @type {number}
     * @memberof GetCharactersCharacterIdPlanetsPlanetIdRoute
     */
    routeId: number;
    /**
     * source_pin_id integer
     * @type {number}
     * @memberof GetCharactersCharacterIdPlanetsPlanetIdRoute
     */
    sourcePinId: number;
    /**
     * list of pin ID waypoints
     * @type {Array<number>}
     * @memberof GetCharactersCharacterIdPlanetsPlanetIdRoute
     */
    waypoints?: Array<number>;
}

/**
 * Check if a given object implements the GetCharactersCharacterIdPlanetsPlanetIdRoute interface.
 */
export function instanceOfGetCharactersCharacterIdPlanetsPlanetIdRoute(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "contentTypeId" in value;
    isInstance = isInstance && "destinationPinId" in value;
    isInstance = isInstance && "quantity" in value;
    isInstance = isInstance && "routeId" in value;
    isInstance = isInstance && "sourcePinId" in value;

    return isInstance;
}

export function GetCharactersCharacterIdPlanetsPlanetIdRouteFromJSON(json: any): GetCharactersCharacterIdPlanetsPlanetIdRoute {
    return GetCharactersCharacterIdPlanetsPlanetIdRouteFromJSONTyped(json, false);
}

export function GetCharactersCharacterIdPlanetsPlanetIdRouteFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCharactersCharacterIdPlanetsPlanetIdRoute {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'contentTypeId': json['content_type_id'],
        'destinationPinId': json['destination_pin_id'],
        'quantity': json['quantity'],
        'routeId': json['route_id'],
        'sourcePinId': json['source_pin_id'],
        'waypoints': !exists(json, 'waypoints') ? undefined : json['waypoints'],
    };
}

export function GetCharactersCharacterIdPlanetsPlanetIdRouteToJSON(value?: GetCharactersCharacterIdPlanetsPlanetIdRoute | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'content_type_id': value.contentTypeId,
        'destination_pin_id': value.destinationPinId,
        'quantity': value.quantity,
        'route_id': value.routeId,
        'source_pin_id': value.sourcePinId,
        'waypoints': value.waypoints,
    };
}
