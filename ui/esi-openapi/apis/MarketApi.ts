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


import * as runtime from '../runtime';
import type {
  BadRequest,
  ErrorLimited,
  Forbidden,
  GatewayTimeout,
  GetCharactersCharacterIdOrders200Ok,
  GetCharactersCharacterIdOrdersHistory200Ok,
  GetCorporationsCorporationIdOrders200Ok,
  GetCorporationsCorporationIdOrdersHistory200Ok,
  GetMarketsGroupsMarketGroupIdNotFound,
  GetMarketsGroupsMarketGroupIdOk,
  GetMarketsPrices200Ok,
  GetMarketsRegionIdHistory200Ok,
  GetMarketsRegionIdHistoryError520,
  GetMarketsRegionIdHistoryNotFound,
  GetMarketsRegionIdHistoryUnprocessableEntity,
  GetMarketsRegionIdOrders200Ok,
  GetMarketsRegionIdOrdersNotFound,
  GetMarketsRegionIdOrdersUnprocessableEntity,
  GetMarketsRegionIdTypesNotFound,
  GetMarketsStructuresStructureId200Ok,
  InternalServerError,
  ServiceUnavailable,
  Unauthorized,
} from '../models';
import {
    BadRequestFromJSON,
    BadRequestToJSON,
    ErrorLimitedFromJSON,
    ErrorLimitedToJSON,
    ForbiddenFromJSON,
    ForbiddenToJSON,
    GatewayTimeoutFromJSON,
    GatewayTimeoutToJSON,
    GetCharactersCharacterIdOrders200OkFromJSON,
    GetCharactersCharacterIdOrders200OkToJSON,
    GetCharactersCharacterIdOrdersHistory200OkFromJSON,
    GetCharactersCharacterIdOrdersHistory200OkToJSON,
    GetCorporationsCorporationIdOrders200OkFromJSON,
    GetCorporationsCorporationIdOrders200OkToJSON,
    GetCorporationsCorporationIdOrdersHistory200OkFromJSON,
    GetCorporationsCorporationIdOrdersHistory200OkToJSON,
    GetMarketsGroupsMarketGroupIdNotFoundFromJSON,
    GetMarketsGroupsMarketGroupIdNotFoundToJSON,
    GetMarketsGroupsMarketGroupIdOkFromJSON,
    GetMarketsGroupsMarketGroupIdOkToJSON,
    GetMarketsPrices200OkFromJSON,
    GetMarketsPrices200OkToJSON,
    GetMarketsRegionIdHistory200OkFromJSON,
    GetMarketsRegionIdHistory200OkToJSON,
    GetMarketsRegionIdHistoryError520FromJSON,
    GetMarketsRegionIdHistoryError520ToJSON,
    GetMarketsRegionIdHistoryNotFoundFromJSON,
    GetMarketsRegionIdHistoryNotFoundToJSON,
    GetMarketsRegionIdHistoryUnprocessableEntityFromJSON,
    GetMarketsRegionIdHistoryUnprocessableEntityToJSON,
    GetMarketsRegionIdOrders200OkFromJSON,
    GetMarketsRegionIdOrders200OkToJSON,
    GetMarketsRegionIdOrdersNotFoundFromJSON,
    GetMarketsRegionIdOrdersNotFoundToJSON,
    GetMarketsRegionIdOrdersUnprocessableEntityFromJSON,
    GetMarketsRegionIdOrdersUnprocessableEntityToJSON,
    GetMarketsRegionIdTypesNotFoundFromJSON,
    GetMarketsRegionIdTypesNotFoundToJSON,
    GetMarketsStructuresStructureId200OkFromJSON,
    GetMarketsStructuresStructureId200OkToJSON,
    InternalServerErrorFromJSON,
    InternalServerErrorToJSON,
    ServiceUnavailableFromJSON,
    ServiceUnavailableToJSON,
    UnauthorizedFromJSON,
    UnauthorizedToJSON,
} from '../models';

export interface GetCharactersCharacterIdOrdersRequest {
    characterId: number;
    datasource?: GetCharactersCharacterIdOrdersDatasourceEnum;
    ifNoneMatch?: string;
    token?: string;
}

export interface GetCharactersCharacterIdOrdersHistoryRequest {
    characterId: number;
    datasource?: GetCharactersCharacterIdOrdersHistoryDatasourceEnum;
    ifNoneMatch?: string;
    page?: number;
    token?: string;
}

export interface GetCorporationsCorporationIdOrdersRequest {
    corporationId: number;
    datasource?: GetCorporationsCorporationIdOrdersDatasourceEnum;
    ifNoneMatch?: string;
    page?: number;
    token?: string;
}

export interface GetCorporationsCorporationIdOrdersHistoryRequest {
    corporationId: number;
    datasource?: GetCorporationsCorporationIdOrdersHistoryDatasourceEnum;
    ifNoneMatch?: string;
    page?: number;
    token?: string;
}

export interface GetMarketsGroupsRequest {
    datasource?: GetMarketsGroupsDatasourceEnum;
    ifNoneMatch?: string;
}

export interface GetMarketsGroupsMarketGroupIdRequest {
    marketGroupId: number;
    acceptLanguage?: GetMarketsGroupsMarketGroupIdAcceptLanguageEnum;
    datasource?: GetMarketsGroupsMarketGroupIdDatasourceEnum;
    ifNoneMatch?: string;
    language?: GetMarketsGroupsMarketGroupIdLanguageEnum;
}

export interface GetMarketsPricesRequest {
    datasource?: GetMarketsPricesDatasourceEnum;
    ifNoneMatch?: string;
}

export interface GetMarketsRegionIdHistoryRequest {
    regionId: number;
    typeId: number;
    datasource?: GetMarketsRegionIdHistoryDatasourceEnum;
    ifNoneMatch?: string;
}

export interface GetMarketsRegionIdOrdersRequest {
    orderType: GetMarketsRegionIdOrdersOrderTypeEnum;
    regionId: number;
    datasource?: GetMarketsRegionIdOrdersDatasourceEnum;
    ifNoneMatch?: string;
    page?: number;
    typeId?: number;
}

export interface GetMarketsRegionIdTypesRequest {
    regionId: number;
    datasource?: GetMarketsRegionIdTypesDatasourceEnum;
    ifNoneMatch?: string;
    page?: number;
}

export interface GetMarketsStructuresStructureIdRequest {
    structureId: number;
    datasource?: GetMarketsStructuresStructureIdDatasourceEnum;
    ifNoneMatch?: string;
    page?: number;
    token?: string;
}

/**
 * 
 */
export class MarketApi extends runtime.BaseAPI {

    /**
     * List open market orders placed by a character  --- Alternate route: `/dev/characters/{character_id}/orders/`  Alternate route: `/v2/characters/{character_id}/orders/`  --- This route is cached for up to 1200 seconds
     * List open orders from a character
     */
    async getCharactersCharacterIdOrdersRaw(requestParameters: GetCharactersCharacterIdOrdersRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<GetCharactersCharacterIdOrders200Ok>>> {
        if (requestParameters.characterId === null || requestParameters.characterId === undefined) {
            throw new runtime.RequiredError('characterId','Required parameter requestParameters.characterId was null or undefined when calling getCharactersCharacterIdOrders.');
        }

        const queryParameters: any = {};

        if (requestParameters.datasource !== undefined) {
            queryParameters['datasource'] = requestParameters.datasource;
        }

        if (requestParameters.token !== undefined) {
            queryParameters['token'] = requestParameters.token;
        }

        const headerParameters: runtime.HTTPHeaders = {};

        if (requestParameters.ifNoneMatch !== undefined && requestParameters.ifNoneMatch !== null) {
            headerParameters['If-None-Match'] = String(requestParameters.ifNoneMatch);
        }

        if (this.configuration && this.configuration.accessToken) {
            // oauth required
            headerParameters["Authorization"] = await this.configuration.accessToken("evesso", ["esi-markets.read_character_orders.v1"]);
        }

        const response = await this.request({
            path: `/characters/{character_id}/orders/`.replace(`{${"character_id"}}`, encodeURIComponent(String(requestParameters.characterId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(GetCharactersCharacterIdOrders200OkFromJSON));
    }

    /**
     * List open market orders placed by a character  --- Alternate route: `/dev/characters/{character_id}/orders/`  Alternate route: `/v2/characters/{character_id}/orders/`  --- This route is cached for up to 1200 seconds
     * List open orders from a character
     */
    async getCharactersCharacterIdOrders(requestParameters: GetCharactersCharacterIdOrdersRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<GetCharactersCharacterIdOrders200Ok>> {
        const response = await this.getCharactersCharacterIdOrdersRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * List cancelled and expired market orders placed by a character up to 90 days in the past.  --- Alternate route: `/dev/characters/{character_id}/orders/history/`  Alternate route: `/legacy/characters/{character_id}/orders/history/`  Alternate route: `/v1/characters/{character_id}/orders/history/`  --- This route is cached for up to 3600 seconds
     * List historical orders by a character
     */
    async getCharactersCharacterIdOrdersHistoryRaw(requestParameters: GetCharactersCharacterIdOrdersHistoryRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<GetCharactersCharacterIdOrdersHistory200Ok>>> {
        if (requestParameters.characterId === null || requestParameters.characterId === undefined) {
            throw new runtime.RequiredError('characterId','Required parameter requestParameters.characterId was null or undefined when calling getCharactersCharacterIdOrdersHistory.');
        }

        const queryParameters: any = {};

        if (requestParameters.datasource !== undefined) {
            queryParameters['datasource'] = requestParameters.datasource;
        }

        if (requestParameters.page !== undefined) {
            queryParameters['page'] = requestParameters.page;
        }

        if (requestParameters.token !== undefined) {
            queryParameters['token'] = requestParameters.token;
        }

        const headerParameters: runtime.HTTPHeaders = {};

        if (requestParameters.ifNoneMatch !== undefined && requestParameters.ifNoneMatch !== null) {
            headerParameters['If-None-Match'] = String(requestParameters.ifNoneMatch);
        }

        if (this.configuration && this.configuration.accessToken) {
            // oauth required
            headerParameters["Authorization"] = await this.configuration.accessToken("evesso", ["esi-markets.read_character_orders.v1"]);
        }

        const response = await this.request({
            path: `/characters/{character_id}/orders/history/`.replace(`{${"character_id"}}`, encodeURIComponent(String(requestParameters.characterId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(GetCharactersCharacterIdOrdersHistory200OkFromJSON));
    }

    /**
     * List cancelled and expired market orders placed by a character up to 90 days in the past.  --- Alternate route: `/dev/characters/{character_id}/orders/history/`  Alternate route: `/legacy/characters/{character_id}/orders/history/`  Alternate route: `/v1/characters/{character_id}/orders/history/`  --- This route is cached for up to 3600 seconds
     * List historical orders by a character
     */
    async getCharactersCharacterIdOrdersHistory(requestParameters: GetCharactersCharacterIdOrdersHistoryRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<GetCharactersCharacterIdOrdersHistory200Ok>> {
        const response = await this.getCharactersCharacterIdOrdersHistoryRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * List open market orders placed on behalf of a corporation  --- Alternate route: `/dev/corporations/{corporation_id}/orders/`  Alternate route: `/v3/corporations/{corporation_id}/orders/`  --- This route is cached for up to 1200 seconds  --- Requires one of the following EVE corporation role(s): Accountant, Trader 
     * List open orders from a corporation
     */
    async getCorporationsCorporationIdOrdersRaw(requestParameters: GetCorporationsCorporationIdOrdersRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<GetCorporationsCorporationIdOrders200Ok>>> {
        if (requestParameters.corporationId === null || requestParameters.corporationId === undefined) {
            throw new runtime.RequiredError('corporationId','Required parameter requestParameters.corporationId was null or undefined when calling getCorporationsCorporationIdOrders.');
        }

        const queryParameters: any = {};

        if (requestParameters.datasource !== undefined) {
            queryParameters['datasource'] = requestParameters.datasource;
        }

        if (requestParameters.page !== undefined) {
            queryParameters['page'] = requestParameters.page;
        }

        if (requestParameters.token !== undefined) {
            queryParameters['token'] = requestParameters.token;
        }

        const headerParameters: runtime.HTTPHeaders = {};

        if (requestParameters.ifNoneMatch !== undefined && requestParameters.ifNoneMatch !== null) {
            headerParameters['If-None-Match'] = String(requestParameters.ifNoneMatch);
        }

        if (this.configuration && this.configuration.accessToken) {
            // oauth required
            headerParameters["Authorization"] = await this.configuration.accessToken("evesso", ["esi-markets.read_corporation_orders.v1"]);
        }

        const response = await this.request({
            path: `/corporations/{corporation_id}/orders/`.replace(`{${"corporation_id"}}`, encodeURIComponent(String(requestParameters.corporationId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(GetCorporationsCorporationIdOrders200OkFromJSON));
    }

    /**
     * List open market orders placed on behalf of a corporation  --- Alternate route: `/dev/corporations/{corporation_id}/orders/`  Alternate route: `/v3/corporations/{corporation_id}/orders/`  --- This route is cached for up to 1200 seconds  --- Requires one of the following EVE corporation role(s): Accountant, Trader 
     * List open orders from a corporation
     */
    async getCorporationsCorporationIdOrders(requestParameters: GetCorporationsCorporationIdOrdersRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<GetCorporationsCorporationIdOrders200Ok>> {
        const response = await this.getCorporationsCorporationIdOrdersRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * List cancelled and expired market orders placed on behalf of a corporation up to 90 days in the past.  --- Alternate route: `/dev/corporations/{corporation_id}/orders/history/`  Alternate route: `/v2/corporations/{corporation_id}/orders/history/`  --- This route is cached for up to 3600 seconds  --- Requires one of the following EVE corporation role(s): Accountant, Trader 
     * List historical orders from a corporation
     */
    async getCorporationsCorporationIdOrdersHistoryRaw(requestParameters: GetCorporationsCorporationIdOrdersHistoryRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<GetCorporationsCorporationIdOrdersHistory200Ok>>> {
        if (requestParameters.corporationId === null || requestParameters.corporationId === undefined) {
            throw new runtime.RequiredError('corporationId','Required parameter requestParameters.corporationId was null or undefined when calling getCorporationsCorporationIdOrdersHistory.');
        }

        const queryParameters: any = {};

        if (requestParameters.datasource !== undefined) {
            queryParameters['datasource'] = requestParameters.datasource;
        }

        if (requestParameters.page !== undefined) {
            queryParameters['page'] = requestParameters.page;
        }

        if (requestParameters.token !== undefined) {
            queryParameters['token'] = requestParameters.token;
        }

        const headerParameters: runtime.HTTPHeaders = {};

        if (requestParameters.ifNoneMatch !== undefined && requestParameters.ifNoneMatch !== null) {
            headerParameters['If-None-Match'] = String(requestParameters.ifNoneMatch);
        }

        if (this.configuration && this.configuration.accessToken) {
            // oauth required
            headerParameters["Authorization"] = await this.configuration.accessToken("evesso", ["esi-markets.read_corporation_orders.v1"]);
        }

        const response = await this.request({
            path: `/corporations/{corporation_id}/orders/history/`.replace(`{${"corporation_id"}}`, encodeURIComponent(String(requestParameters.corporationId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(GetCorporationsCorporationIdOrdersHistory200OkFromJSON));
    }

    /**
     * List cancelled and expired market orders placed on behalf of a corporation up to 90 days in the past.  --- Alternate route: `/dev/corporations/{corporation_id}/orders/history/`  Alternate route: `/v2/corporations/{corporation_id}/orders/history/`  --- This route is cached for up to 3600 seconds  --- Requires one of the following EVE corporation role(s): Accountant, Trader 
     * List historical orders from a corporation
     */
    async getCorporationsCorporationIdOrdersHistory(requestParameters: GetCorporationsCorporationIdOrdersHistoryRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<GetCorporationsCorporationIdOrdersHistory200Ok>> {
        const response = await this.getCorporationsCorporationIdOrdersHistoryRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Get a list of item groups  --- Alternate route: `/dev/markets/groups/`  Alternate route: `/legacy/markets/groups/`  Alternate route: `/v1/markets/groups/`  --- This route expires daily at 11:05
     * Get item groups
     */
    async getMarketsGroupsRaw(requestParameters: GetMarketsGroupsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<number>>> {
        const queryParameters: any = {};

        if (requestParameters.datasource !== undefined) {
            queryParameters['datasource'] = requestParameters.datasource;
        }

        const headerParameters: runtime.HTTPHeaders = {};

        if (requestParameters.ifNoneMatch !== undefined && requestParameters.ifNoneMatch !== null) {
            headerParameters['If-None-Match'] = String(requestParameters.ifNoneMatch);
        }

        const response = await this.request({
            path: `/markets/groups/`,
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse<any>(response);
    }

    /**
     * Get a list of item groups  --- Alternate route: `/dev/markets/groups/`  Alternate route: `/legacy/markets/groups/`  Alternate route: `/v1/markets/groups/`  --- This route expires daily at 11:05
     * Get item groups
     */
    async getMarketsGroups(requestParameters: GetMarketsGroupsRequest = {}, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<number>> {
        const response = await this.getMarketsGroupsRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Get information on an item group  --- Alternate route: `/dev/markets/groups/{market_group_id}/`  Alternate route: `/legacy/markets/groups/{market_group_id}/`  Alternate route: `/v1/markets/groups/{market_group_id}/`  --- This route expires daily at 11:05
     * Get item group information
     */
    async getMarketsGroupsMarketGroupIdRaw(requestParameters: GetMarketsGroupsMarketGroupIdRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<GetMarketsGroupsMarketGroupIdOk>> {
        if (requestParameters.marketGroupId === null || requestParameters.marketGroupId === undefined) {
            throw new runtime.RequiredError('marketGroupId','Required parameter requestParameters.marketGroupId was null or undefined when calling getMarketsGroupsMarketGroupId.');
        }

        const queryParameters: any = {};

        if (requestParameters.datasource !== undefined) {
            queryParameters['datasource'] = requestParameters.datasource;
        }

        if (requestParameters.language !== undefined) {
            queryParameters['language'] = requestParameters.language;
        }

        const headerParameters: runtime.HTTPHeaders = {};

        if (requestParameters.acceptLanguage !== undefined && requestParameters.acceptLanguage !== null) {
            headerParameters['Accept-Language'] = String(requestParameters.acceptLanguage);
        }

        if (requestParameters.ifNoneMatch !== undefined && requestParameters.ifNoneMatch !== null) {
            headerParameters['If-None-Match'] = String(requestParameters.ifNoneMatch);
        }

        const response = await this.request({
            path: `/markets/groups/{market_group_id}/`.replace(`{${"market_group_id"}}`, encodeURIComponent(String(requestParameters.marketGroupId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => GetMarketsGroupsMarketGroupIdOkFromJSON(jsonValue));
    }

    /**
     * Get information on an item group  --- Alternate route: `/dev/markets/groups/{market_group_id}/`  Alternate route: `/legacy/markets/groups/{market_group_id}/`  Alternate route: `/v1/markets/groups/{market_group_id}/`  --- This route expires daily at 11:05
     * Get item group information
     */
    async getMarketsGroupsMarketGroupId(requestParameters: GetMarketsGroupsMarketGroupIdRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<GetMarketsGroupsMarketGroupIdOk> {
        const response = await this.getMarketsGroupsMarketGroupIdRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Return a list of prices  --- Alternate route: `/dev/markets/prices/`  Alternate route: `/legacy/markets/prices/`  Alternate route: `/v1/markets/prices/`  --- This route is cached for up to 3600 seconds
     * List market prices
     */
    async getMarketsPricesRaw(requestParameters: GetMarketsPricesRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<GetMarketsPrices200Ok>>> {
        const queryParameters: any = {};

        if (requestParameters.datasource !== undefined) {
            queryParameters['datasource'] = requestParameters.datasource;
        }

        const headerParameters: runtime.HTTPHeaders = {};

        if (requestParameters.ifNoneMatch !== undefined && requestParameters.ifNoneMatch !== null) {
            headerParameters['If-None-Match'] = String(requestParameters.ifNoneMatch);
        }

        const response = await this.request({
            path: `/markets/prices/`,
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(GetMarketsPrices200OkFromJSON));
    }

    /**
     * Return a list of prices  --- Alternate route: `/dev/markets/prices/`  Alternate route: `/legacy/markets/prices/`  Alternate route: `/v1/markets/prices/`  --- This route is cached for up to 3600 seconds
     * List market prices
     */
    async getMarketsPrices(requestParameters: GetMarketsPricesRequest = {}, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<GetMarketsPrices200Ok>> {
        const response = await this.getMarketsPricesRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Return a list of historical market statistics for the specified type in a region  --- Alternate route: `/dev/markets/{region_id}/history/`  Alternate route: `/legacy/markets/{region_id}/history/`  Alternate route: `/v1/markets/{region_id}/history/`  --- This route expires daily at 11:05
     * List historical market statistics in a region
     */
    async getMarketsRegionIdHistoryRaw(requestParameters: GetMarketsRegionIdHistoryRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<GetMarketsRegionIdHistory200Ok>>> {
        if (requestParameters.regionId === null || requestParameters.regionId === undefined) {
            throw new runtime.RequiredError('regionId','Required parameter requestParameters.regionId was null or undefined when calling getMarketsRegionIdHistory.');
        }

        if (requestParameters.typeId === null || requestParameters.typeId === undefined) {
            throw new runtime.RequiredError('typeId','Required parameter requestParameters.typeId was null or undefined when calling getMarketsRegionIdHistory.');
        }

        const queryParameters: any = {};

        if (requestParameters.datasource !== undefined) {
            queryParameters['datasource'] = requestParameters.datasource;
        }

        if (requestParameters.typeId !== undefined) {
            queryParameters['type_id'] = requestParameters.typeId;
        }

        const headerParameters: runtime.HTTPHeaders = {};

        if (requestParameters.ifNoneMatch !== undefined && requestParameters.ifNoneMatch !== null) {
            headerParameters['If-None-Match'] = String(requestParameters.ifNoneMatch);
        }

        const response = await this.request({
            path: `/markets/{region_id}/history/`.replace(`{${"region_id"}}`, encodeURIComponent(String(requestParameters.regionId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(GetMarketsRegionIdHistory200OkFromJSON));
    }

    /**
     * Return a list of historical market statistics for the specified type in a region  --- Alternate route: `/dev/markets/{region_id}/history/`  Alternate route: `/legacy/markets/{region_id}/history/`  Alternate route: `/v1/markets/{region_id}/history/`  --- This route expires daily at 11:05
     * List historical market statistics in a region
     */
    async getMarketsRegionIdHistory(requestParameters: GetMarketsRegionIdHistoryRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<GetMarketsRegionIdHistory200Ok>> {
        const response = await this.getMarketsRegionIdHistoryRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Return a list of orders in a region  --- Alternate route: `/dev/markets/{region_id}/orders/`  Alternate route: `/legacy/markets/{region_id}/orders/`  Alternate route: `/v1/markets/{region_id}/orders/`  --- This route is cached for up to 300 seconds
     * List orders in a region
     */
    async getMarketsRegionIdOrdersRaw(requestParameters: GetMarketsRegionIdOrdersRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<GetMarketsRegionIdOrders200Ok>>> {
        if (requestParameters.orderType === null || requestParameters.orderType === undefined) {
            throw new runtime.RequiredError('orderType','Required parameter requestParameters.orderType was null or undefined when calling getMarketsRegionIdOrders.');
        }

        if (requestParameters.regionId === null || requestParameters.regionId === undefined) {
            throw new runtime.RequiredError('regionId','Required parameter requestParameters.regionId was null or undefined when calling getMarketsRegionIdOrders.');
        }

        const queryParameters: any = {};

        if (requestParameters.datasource !== undefined) {
            queryParameters['datasource'] = requestParameters.datasource;
        }

        if (requestParameters.orderType !== undefined) {
            queryParameters['order_type'] = requestParameters.orderType;
        }

        if (requestParameters.page !== undefined) {
            queryParameters['page'] = requestParameters.page;
        }

        if (requestParameters.typeId !== undefined) {
            queryParameters['type_id'] = requestParameters.typeId;
        }

        const headerParameters: runtime.HTTPHeaders = {};

        if (requestParameters.ifNoneMatch !== undefined && requestParameters.ifNoneMatch !== null) {
            headerParameters['If-None-Match'] = String(requestParameters.ifNoneMatch);
        }

        const response = await this.request({
            path: `/markets/{region_id}/orders/`.replace(`{${"region_id"}}`, encodeURIComponent(String(requestParameters.regionId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(GetMarketsRegionIdOrders200OkFromJSON));
    }

    /**
     * Return a list of orders in a region  --- Alternate route: `/dev/markets/{region_id}/orders/`  Alternate route: `/legacy/markets/{region_id}/orders/`  Alternate route: `/v1/markets/{region_id}/orders/`  --- This route is cached for up to 300 seconds
     * List orders in a region
     */
    async getMarketsRegionIdOrders(requestParameters: GetMarketsRegionIdOrdersRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<GetMarketsRegionIdOrders200Ok>> {
        const response = await this.getMarketsRegionIdOrdersRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Return a list of type IDs that have active orders in the region, for efficient market indexing.  --- Alternate route: `/dev/markets/{region_id}/types/`  Alternate route: `/legacy/markets/{region_id}/types/`  Alternate route: `/v1/markets/{region_id}/types/`  --- This route is cached for up to 600 seconds
     * List type IDs relevant to a market
     */
    async getMarketsRegionIdTypesRaw(requestParameters: GetMarketsRegionIdTypesRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<number>>> {
        if (requestParameters.regionId === null || requestParameters.regionId === undefined) {
            throw new runtime.RequiredError('regionId','Required parameter requestParameters.regionId was null or undefined when calling getMarketsRegionIdTypes.');
        }

        const queryParameters: any = {};

        if (requestParameters.datasource !== undefined) {
            queryParameters['datasource'] = requestParameters.datasource;
        }

        if (requestParameters.page !== undefined) {
            queryParameters['page'] = requestParameters.page;
        }

        const headerParameters: runtime.HTTPHeaders = {};

        if (requestParameters.ifNoneMatch !== undefined && requestParameters.ifNoneMatch !== null) {
            headerParameters['If-None-Match'] = String(requestParameters.ifNoneMatch);
        }

        const response = await this.request({
            path: `/markets/{region_id}/types/`.replace(`{${"region_id"}}`, encodeURIComponent(String(requestParameters.regionId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse<any>(response);
    }

    /**
     * Return a list of type IDs that have active orders in the region, for efficient market indexing.  --- Alternate route: `/dev/markets/{region_id}/types/`  Alternate route: `/legacy/markets/{region_id}/types/`  Alternate route: `/v1/markets/{region_id}/types/`  --- This route is cached for up to 600 seconds
     * List type IDs relevant to a market
     */
    async getMarketsRegionIdTypes(requestParameters: GetMarketsRegionIdTypesRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<number>> {
        const response = await this.getMarketsRegionIdTypesRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Return all orders in a structure  --- Alternate route: `/dev/markets/structures/{structure_id}/`  Alternate route: `/legacy/markets/structures/{structure_id}/`  Alternate route: `/v1/markets/structures/{structure_id}/`  --- This route is cached for up to 300 seconds
     * List orders in a structure
     */
    async getMarketsStructuresStructureIdRaw(requestParameters: GetMarketsStructuresStructureIdRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<GetMarketsStructuresStructureId200Ok>>> {
        if (requestParameters.structureId === null || requestParameters.structureId === undefined) {
            throw new runtime.RequiredError('structureId','Required parameter requestParameters.structureId was null or undefined when calling getMarketsStructuresStructureId.');
        }

        const queryParameters: any = {};

        if (requestParameters.datasource !== undefined) {
            queryParameters['datasource'] = requestParameters.datasource;
        }

        if (requestParameters.page !== undefined) {
            queryParameters['page'] = requestParameters.page;
        }

        if (requestParameters.token !== undefined) {
            queryParameters['token'] = requestParameters.token;
        }

        const headerParameters: runtime.HTTPHeaders = {};

        if (requestParameters.ifNoneMatch !== undefined && requestParameters.ifNoneMatch !== null) {
            headerParameters['If-None-Match'] = String(requestParameters.ifNoneMatch);
        }

        if (this.configuration && this.configuration.accessToken) {
            // oauth required
            headerParameters["Authorization"] = await this.configuration.accessToken("evesso", ["esi-markets.structure_markets.v1"]);
        }

        const response = await this.request({
            path: `/markets/structures/{structure_id}/`.replace(`{${"structure_id"}}`, encodeURIComponent(String(requestParameters.structureId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(GetMarketsStructuresStructureId200OkFromJSON));
    }

    /**
     * Return all orders in a structure  --- Alternate route: `/dev/markets/structures/{structure_id}/`  Alternate route: `/legacy/markets/structures/{structure_id}/`  Alternate route: `/v1/markets/structures/{structure_id}/`  --- This route is cached for up to 300 seconds
     * List orders in a structure
     */
    async getMarketsStructuresStructureId(requestParameters: GetMarketsStructuresStructureIdRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<GetMarketsStructuresStructureId200Ok>> {
        const response = await this.getMarketsStructuresStructureIdRaw(requestParameters, initOverrides);
        return await response.value();
    }

}

/**
 * @export
 */
export const GetCharactersCharacterIdOrdersDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type GetCharactersCharacterIdOrdersDatasourceEnum = typeof GetCharactersCharacterIdOrdersDatasourceEnum[keyof typeof GetCharactersCharacterIdOrdersDatasourceEnum];
/**
 * @export
 */
export const GetCharactersCharacterIdOrdersHistoryDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type GetCharactersCharacterIdOrdersHistoryDatasourceEnum = typeof GetCharactersCharacterIdOrdersHistoryDatasourceEnum[keyof typeof GetCharactersCharacterIdOrdersHistoryDatasourceEnum];
/**
 * @export
 */
export const GetCorporationsCorporationIdOrdersDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type GetCorporationsCorporationIdOrdersDatasourceEnum = typeof GetCorporationsCorporationIdOrdersDatasourceEnum[keyof typeof GetCorporationsCorporationIdOrdersDatasourceEnum];
/**
 * @export
 */
export const GetCorporationsCorporationIdOrdersHistoryDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type GetCorporationsCorporationIdOrdersHistoryDatasourceEnum = typeof GetCorporationsCorporationIdOrdersHistoryDatasourceEnum[keyof typeof GetCorporationsCorporationIdOrdersHistoryDatasourceEnum];
/**
 * @export
 */
export const GetMarketsGroupsDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type GetMarketsGroupsDatasourceEnum = typeof GetMarketsGroupsDatasourceEnum[keyof typeof GetMarketsGroupsDatasourceEnum];
/**
 * @export
 */
export const GetMarketsGroupsMarketGroupIdAcceptLanguageEnum = {
    En: 'en',
    EnUs: 'en-us',
    De: 'de',
    Fr: 'fr',
    Ja: 'ja',
    Ru: 'ru',
    Zh: 'zh',
    Ko: 'ko',
    Es: 'es'
} as const;
export type GetMarketsGroupsMarketGroupIdAcceptLanguageEnum = typeof GetMarketsGroupsMarketGroupIdAcceptLanguageEnum[keyof typeof GetMarketsGroupsMarketGroupIdAcceptLanguageEnum];
/**
 * @export
 */
export const GetMarketsGroupsMarketGroupIdDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type GetMarketsGroupsMarketGroupIdDatasourceEnum = typeof GetMarketsGroupsMarketGroupIdDatasourceEnum[keyof typeof GetMarketsGroupsMarketGroupIdDatasourceEnum];
/**
 * @export
 */
export const GetMarketsGroupsMarketGroupIdLanguageEnum = {
    En: 'en',
    EnUs: 'en-us',
    De: 'de',
    Fr: 'fr',
    Ja: 'ja',
    Ru: 'ru',
    Zh: 'zh',
    Ko: 'ko',
    Es: 'es'
} as const;
export type GetMarketsGroupsMarketGroupIdLanguageEnum = typeof GetMarketsGroupsMarketGroupIdLanguageEnum[keyof typeof GetMarketsGroupsMarketGroupIdLanguageEnum];
/**
 * @export
 */
export const GetMarketsPricesDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type GetMarketsPricesDatasourceEnum = typeof GetMarketsPricesDatasourceEnum[keyof typeof GetMarketsPricesDatasourceEnum];
/**
 * @export
 */
export const GetMarketsRegionIdHistoryDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type GetMarketsRegionIdHistoryDatasourceEnum = typeof GetMarketsRegionIdHistoryDatasourceEnum[keyof typeof GetMarketsRegionIdHistoryDatasourceEnum];
/**
 * @export
 */
export const GetMarketsRegionIdOrdersOrderTypeEnum = {
    Buy: 'buy',
    Sell: 'sell',
    All: 'all'
} as const;
export type GetMarketsRegionIdOrdersOrderTypeEnum = typeof GetMarketsRegionIdOrdersOrderTypeEnum[keyof typeof GetMarketsRegionIdOrdersOrderTypeEnum];
/**
 * @export
 */
export const GetMarketsRegionIdOrdersDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type GetMarketsRegionIdOrdersDatasourceEnum = typeof GetMarketsRegionIdOrdersDatasourceEnum[keyof typeof GetMarketsRegionIdOrdersDatasourceEnum];
/**
 * @export
 */
export const GetMarketsRegionIdTypesDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type GetMarketsRegionIdTypesDatasourceEnum = typeof GetMarketsRegionIdTypesDatasourceEnum[keyof typeof GetMarketsRegionIdTypesDatasourceEnum];
/**
 * @export
 */
export const GetMarketsStructuresStructureIdDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type GetMarketsStructuresStructureIdDatasourceEnum = typeof GetMarketsStructuresStructureIdDatasourceEnum[keyof typeof GetMarketsStructuresStructureIdDatasourceEnum];
