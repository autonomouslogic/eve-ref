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
 * @interface GetCorporationsCorporationIdOrdersHistory200Ok
 */
export interface GetCorporationsCorporationIdOrdersHistory200Ok {
    /**
     * Number of days the order was valid for (starting from the issued date). An order expires at time issued + duration
     * @type {number}
     * @memberof GetCorporationsCorporationIdOrdersHistory200Ok
     */
    duration: number;
    /**
     * For buy orders, the amount of ISK in escrow
     * @type {number}
     * @memberof GetCorporationsCorporationIdOrdersHistory200Ok
     */
    escrow?: number;
    /**
     * True if the order is a bid (buy) order
     * @type {boolean}
     * @memberof GetCorporationsCorporationIdOrdersHistory200Ok
     */
    isBuyOrder?: boolean;
    /**
     * Date and time when this order was issued
     * @type {Date}
     * @memberof GetCorporationsCorporationIdOrdersHistory200Ok
     */
    issued: Date;
    /**
     * The character who issued this order
     * @type {number}
     * @memberof GetCorporationsCorporationIdOrdersHistory200Ok
     */
    issuedBy?: number;
    /**
     * ID of the location where order was placed
     * @type {number}
     * @memberof GetCorporationsCorporationIdOrdersHistory200Ok
     */
    locationId: number;
    /**
     * For buy orders, the minimum quantity that will be accepted in a matching sell order
     * @type {number}
     * @memberof GetCorporationsCorporationIdOrdersHistory200Ok
     */
    minVolume?: number;
    /**
     * Unique order ID
     * @type {number}
     * @memberof GetCorporationsCorporationIdOrdersHistory200Ok
     */
    orderId: number;
    /**
     * Cost per unit for this order
     * @type {number}
     * @memberof GetCorporationsCorporationIdOrdersHistory200Ok
     */
    price: number;
    /**
     * Valid order range, numbers are ranges in jumps
     * @type {string}
     * @memberof GetCorporationsCorporationIdOrdersHistory200Ok
     */
    range: GetCorporationsCorporationIdOrdersHistory200OkRangeEnum;
    /**
     * ID of the region where order was placed
     * @type {number}
     * @memberof GetCorporationsCorporationIdOrdersHistory200Ok
     */
    regionId: number;
    /**
     * Current order state
     * @type {string}
     * @memberof GetCorporationsCorporationIdOrdersHistory200Ok
     */
    state: GetCorporationsCorporationIdOrdersHistory200OkStateEnum;
    /**
     * The type ID of the item transacted in this order
     * @type {number}
     * @memberof GetCorporationsCorporationIdOrdersHistory200Ok
     */
    typeId: number;
    /**
     * Quantity of items still required or offered
     * @type {number}
     * @memberof GetCorporationsCorporationIdOrdersHistory200Ok
     */
    volumeRemain: number;
    /**
     * Quantity of items required or offered at time order was placed
     * @type {number}
     * @memberof GetCorporationsCorporationIdOrdersHistory200Ok
     */
    volumeTotal: number;
    /**
     * The corporation wallet division used for this order
     * @type {number}
     * @memberof GetCorporationsCorporationIdOrdersHistory200Ok
     */
    walletDivision: number;
}


/**
 * @export
 */
export const GetCorporationsCorporationIdOrdersHistory200OkRangeEnum = {
    _1: '1',
    _10: '10',
    _2: '2',
    _20: '20',
    _3: '3',
    _30: '30',
    _4: '4',
    _40: '40',
    _5: '5',
    Region: 'region',
    Solarsystem: 'solarsystem',
    Station: 'station'
} as const;
export type GetCorporationsCorporationIdOrdersHistory200OkRangeEnum = typeof GetCorporationsCorporationIdOrdersHistory200OkRangeEnum[keyof typeof GetCorporationsCorporationIdOrdersHistory200OkRangeEnum];

/**
 * @export
 */
export const GetCorporationsCorporationIdOrdersHistory200OkStateEnum = {
    Cancelled: 'cancelled',
    Expired: 'expired'
} as const;
export type GetCorporationsCorporationIdOrdersHistory200OkStateEnum = typeof GetCorporationsCorporationIdOrdersHistory200OkStateEnum[keyof typeof GetCorporationsCorporationIdOrdersHistory200OkStateEnum];


/**
 * Check if a given object implements the GetCorporationsCorporationIdOrdersHistory200Ok interface.
 */
export function instanceOfGetCorporationsCorporationIdOrdersHistory200Ok(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "duration" in value;
    isInstance = isInstance && "issued" in value;
    isInstance = isInstance && "locationId" in value;
    isInstance = isInstance && "orderId" in value;
    isInstance = isInstance && "price" in value;
    isInstance = isInstance && "range" in value;
    isInstance = isInstance && "regionId" in value;
    isInstance = isInstance && "state" in value;
    isInstance = isInstance && "typeId" in value;
    isInstance = isInstance && "volumeRemain" in value;
    isInstance = isInstance && "volumeTotal" in value;
    isInstance = isInstance && "walletDivision" in value;

    return isInstance;
}

export function GetCorporationsCorporationIdOrdersHistory200OkFromJSON(json: any): GetCorporationsCorporationIdOrdersHistory200Ok {
    return GetCorporationsCorporationIdOrdersHistory200OkFromJSONTyped(json, false);
}

export function GetCorporationsCorporationIdOrdersHistory200OkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCorporationsCorporationIdOrdersHistory200Ok {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'duration': json['duration'],
        'escrow': !exists(json, 'escrow') ? undefined : json['escrow'],
        'isBuyOrder': !exists(json, 'is_buy_order') ? undefined : json['is_buy_order'],
        'issued': (new Date(json['issued'])),
        'issuedBy': !exists(json, 'issued_by') ? undefined : json['issued_by'],
        'locationId': json['location_id'],
        'minVolume': !exists(json, 'min_volume') ? undefined : json['min_volume'],
        'orderId': json['order_id'],
        'price': json['price'],
        'range': json['range'],
        'regionId': json['region_id'],
        'state': json['state'],
        'typeId': json['type_id'],
        'volumeRemain': json['volume_remain'],
        'volumeTotal': json['volume_total'],
        'walletDivision': json['wallet_division'],
    };
}

export function GetCorporationsCorporationIdOrdersHistory200OkToJSON(value?: GetCorporationsCorporationIdOrdersHistory200Ok | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'duration': value.duration,
        'escrow': value.escrow,
        'is_buy_order': value.isBuyOrder,
        'issued': (value.issued.toISOString()),
        'issued_by': value.issuedBy,
        'location_id': value.locationId,
        'min_volume': value.minVolume,
        'order_id': value.orderId,
        'price': value.price,
        'range': value.range,
        'region_id': value.regionId,
        'state': value.state,
        'type_id': value.typeId,
        'volume_remain': value.volumeRemain,
        'volume_total': value.volumeTotal,
        'wallet_division': value.walletDivision,
    };
}

