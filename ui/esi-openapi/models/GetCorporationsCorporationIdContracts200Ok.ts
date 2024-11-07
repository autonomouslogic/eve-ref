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
/**
 * 200 ok object
 * @export
 * @interface GetCorporationsCorporationIdContracts200Ok
 */
export interface GetCorporationsCorporationIdContracts200Ok {
    /**
     * Who will accept the contract
     * @type {number}
     * @memberof GetCorporationsCorporationIdContracts200Ok
     */
    acceptorId: number;
    /**
     * ID to whom the contract is assigned, can be corporation or character ID
     * @type {number}
     * @memberof GetCorporationsCorporationIdContracts200Ok
     */
    assigneeId: number;
    /**
     * To whom the contract is available
     * @type {string}
     * @memberof GetCorporationsCorporationIdContracts200Ok
     */
    availability: GetCorporationsCorporationIdContracts200OkAvailabilityEnum;
    /**
     * Buyout price (for Auctions only)
     * @type {number}
     * @memberof GetCorporationsCorporationIdContracts200Ok
     */
    buyout?: number;
    /**
     * Collateral price (for Couriers only)
     * @type {number}
     * @memberof GetCorporationsCorporationIdContracts200Ok
     */
    collateral?: number;
    /**
     * contract_id integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdContracts200Ok
     */
    contractId: number;
    /**
     * Date of confirmation of contract
     * @type {Date}
     * @memberof GetCorporationsCorporationIdContracts200Ok
     */
    dateAccepted?: Date;
    /**
     * Date of completed of contract
     * @type {Date}
     * @memberof GetCorporationsCorporationIdContracts200Ok
     */
    dateCompleted?: Date;
    /**
     * Expiration date of the contract
     * @type {Date}
     * @memberof GetCorporationsCorporationIdContracts200Ok
     */
    dateExpired: Date;
    /**
     * Сreation date of the contract
     * @type {Date}
     * @memberof GetCorporationsCorporationIdContracts200Ok
     */
    dateIssued: Date;
    /**
     * Number of days to perform the contract
     * @type {number}
     * @memberof GetCorporationsCorporationIdContracts200Ok
     */
    daysToComplete?: number;
    /**
     * End location ID (for Couriers contract)
     * @type {number}
     * @memberof GetCorporationsCorporationIdContracts200Ok
     */
    endLocationId?: number;
    /**
     * true if the contract was issued on behalf of the issuer's corporation
     * @type {boolean}
     * @memberof GetCorporationsCorporationIdContracts200Ok
     */
    forCorporation: boolean;
    /**
     * Character's corporation ID for the issuer
     * @type {number}
     * @memberof GetCorporationsCorporationIdContracts200Ok
     */
    issuerCorporationId: number;
    /**
     * Character ID for the issuer
     * @type {number}
     * @memberof GetCorporationsCorporationIdContracts200Ok
     */
    issuerId: number;
    /**
     * Price of contract (for ItemsExchange and Auctions)
     * @type {number}
     * @memberof GetCorporationsCorporationIdContracts200Ok
     */
    price?: number;
    /**
     * Remuneration for contract (for Couriers only)
     * @type {number}
     * @memberof GetCorporationsCorporationIdContracts200Ok
     */
    reward?: number;
    /**
     * Start location ID (for Couriers contract)
     * @type {number}
     * @memberof GetCorporationsCorporationIdContracts200Ok
     */
    startLocationId?: number;
    /**
     * Status of the the contract
     * @type {string}
     * @memberof GetCorporationsCorporationIdContracts200Ok
     */
    status: GetCorporationsCorporationIdContracts200OkStatusEnum;
    /**
     * Title of the contract
     * @type {string}
     * @memberof GetCorporationsCorporationIdContracts200Ok
     */
    title?: string;
    /**
     * Type of the contract
     * @type {string}
     * @memberof GetCorporationsCorporationIdContracts200Ok
     */
    type: GetCorporationsCorporationIdContracts200OkTypeEnum;
    /**
     * Volume of items in the contract
     * @type {number}
     * @memberof GetCorporationsCorporationIdContracts200Ok
     */
    volume?: number;
}


/**
 * @export
 */
export const GetCorporationsCorporationIdContracts200OkAvailabilityEnum = {
    Public: 'public',
    Personal: 'personal',
    Corporation: 'corporation',
    Alliance: 'alliance'
} as const;
export type GetCorporationsCorporationIdContracts200OkAvailabilityEnum = typeof GetCorporationsCorporationIdContracts200OkAvailabilityEnum[keyof typeof GetCorporationsCorporationIdContracts200OkAvailabilityEnum];

/**
 * @export
 */
export const GetCorporationsCorporationIdContracts200OkStatusEnum = {
    Outstanding: 'outstanding',
    InProgress: 'in_progress',
    FinishedIssuer: 'finished_issuer',
    FinishedContractor: 'finished_contractor',
    Finished: 'finished',
    Cancelled: 'cancelled',
    Rejected: 'rejected',
    Failed: 'failed',
    Deleted: 'deleted',
    Reversed: 'reversed'
} as const;
export type GetCorporationsCorporationIdContracts200OkStatusEnum = typeof GetCorporationsCorporationIdContracts200OkStatusEnum[keyof typeof GetCorporationsCorporationIdContracts200OkStatusEnum];

/**
 * @export
 */
export const GetCorporationsCorporationIdContracts200OkTypeEnum = {
    Unknown: 'unknown',
    ItemExchange: 'item_exchange',
    Auction: 'auction',
    Courier: 'courier',
    Loan: 'loan'
} as const;
export type GetCorporationsCorporationIdContracts200OkTypeEnum = typeof GetCorporationsCorporationIdContracts200OkTypeEnum[keyof typeof GetCorporationsCorporationIdContracts200OkTypeEnum];


/**
 * Check if a given object implements the GetCorporationsCorporationIdContracts200Ok interface.
 */
export function instanceOfGetCorporationsCorporationIdContracts200Ok(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "acceptorId" in value;
    isInstance = isInstance && "assigneeId" in value;
    isInstance = isInstance && "availability" in value;
    isInstance = isInstance && "contractId" in value;
    isInstance = isInstance && "dateExpired" in value;
    isInstance = isInstance && "dateIssued" in value;
    isInstance = isInstance && "forCorporation" in value;
    isInstance = isInstance && "issuerCorporationId" in value;
    isInstance = isInstance && "issuerId" in value;
    isInstance = isInstance && "status" in value;
    isInstance = isInstance && "type" in value;

    return isInstance;
}

export function GetCorporationsCorporationIdContracts200OkFromJSON(json: any): GetCorporationsCorporationIdContracts200Ok {
    return GetCorporationsCorporationIdContracts200OkFromJSONTyped(json, false);
}

export function GetCorporationsCorporationIdContracts200OkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCorporationsCorporationIdContracts200Ok {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'acceptorId': json['acceptor_id'],
        'assigneeId': json['assignee_id'],
        'availability': json['availability'],
        'buyout': !exists(json, 'buyout') ? undefined : json['buyout'],
        'collateral': !exists(json, 'collateral') ? undefined : json['collateral'],
        'contractId': json['contract_id'],
        'dateAccepted': !exists(json, 'date_accepted') ? undefined : (new Date(json['date_accepted'])),
        'dateCompleted': !exists(json, 'date_completed') ? undefined : (new Date(json['date_completed'])),
        'dateExpired': (new Date(json['date_expired'])),
        'dateIssued': (new Date(json['date_issued'])),
        'daysToComplete': !exists(json, 'days_to_complete') ? undefined : json['days_to_complete'],
        'endLocationId': !exists(json, 'end_location_id') ? undefined : json['end_location_id'],
        'forCorporation': json['for_corporation'],
        'issuerCorporationId': json['issuer_corporation_id'],
        'issuerId': json['issuer_id'],
        'price': !exists(json, 'price') ? undefined : json['price'],
        'reward': !exists(json, 'reward') ? undefined : json['reward'],
        'startLocationId': !exists(json, 'start_location_id') ? undefined : json['start_location_id'],
        'status': json['status'],
        'title': !exists(json, 'title') ? undefined : json['title'],
        'type': json['type'],
        'volume': !exists(json, 'volume') ? undefined : json['volume'],
    };
}

export function GetCorporationsCorporationIdContracts200OkToJSON(value?: GetCorporationsCorporationIdContracts200Ok | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'acceptor_id': value.acceptorId,
        'assignee_id': value.assigneeId,
        'availability': value.availability,
        'buyout': value.buyout,
        'collateral': value.collateral,
        'contract_id': value.contractId,
        'date_accepted': value.dateAccepted === undefined ? undefined : (value.dateAccepted.toISOString()),
        'date_completed': value.dateCompleted === undefined ? undefined : (value.dateCompleted.toISOString()),
        'date_expired': (value.dateExpired.toISOString()),
        'date_issued': (value.dateIssued.toISOString()),
        'days_to_complete': value.daysToComplete,
        'end_location_id': value.endLocationId,
        'for_corporation': value.forCorporation,
        'issuer_corporation_id': value.issuerCorporationId,
        'issuer_id': value.issuerId,
        'price': value.price,
        'reward': value.reward,
        'start_location_id': value.startLocationId,
        'status': value.status,
        'title': value.title,
        'type': value.type,
        'volume': value.volume,
    };
}

