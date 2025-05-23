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
  GatewayTimeout,
  GetRouteOriginDestinationNotFound,
  InternalServerError,
  ServiceUnavailable,
} from '../models';
import {
    BadRequestFromJSON,
    BadRequestToJSON,
    ErrorLimitedFromJSON,
    ErrorLimitedToJSON,
    GatewayTimeoutFromJSON,
    GatewayTimeoutToJSON,
    GetRouteOriginDestinationNotFoundFromJSON,
    GetRouteOriginDestinationNotFoundToJSON,
    InternalServerErrorFromJSON,
    InternalServerErrorToJSON,
    ServiceUnavailableFromJSON,
    ServiceUnavailableToJSON,
} from '../models';

export interface GetRouteOriginDestinationRequest {
    destination: number;
    origin: number;
    avoid?: Set<number>;
    connections?: Set<Set<number>>;
    datasource?: GetRouteOriginDestinationDatasourceEnum;
    flag?: GetRouteOriginDestinationFlagEnum;
    ifNoneMatch?: string;
}

/**
 * 
 */
export class RoutesApi extends runtime.BaseAPI {

    /**
     * Get the systems between origin and destination  --- Alternate route: `/dev/route/{origin}/{destination}/`  Alternate route: `/legacy/route/{origin}/{destination}/`  Alternate route: `/v1/route/{origin}/{destination}/`  --- This route is cached for up to 86400 seconds
     * Get route
     */
    async getRouteOriginDestinationRaw(requestParameters: GetRouteOriginDestinationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<number>>> {
        if (requestParameters.destination === null || requestParameters.destination === undefined) {
            throw new runtime.RequiredError('destination','Required parameter requestParameters.destination was null or undefined when calling getRouteOriginDestination.');
        }

        if (requestParameters.origin === null || requestParameters.origin === undefined) {
            throw new runtime.RequiredError('origin','Required parameter requestParameters.origin was null or undefined when calling getRouteOriginDestination.');
        }

        const queryParameters: any = {};

        if (requestParameters.avoid) {
            queryParameters['avoid'] = Array.from(requestParameters.avoid).join(runtime.COLLECTION_FORMATS["csv"]);
        }

        if (requestParameters.connections) {
            queryParameters['connections'] = Array.from(requestParameters.connections).join(runtime.COLLECTION_FORMATS["csv"]);
        }

        if (requestParameters.datasource !== undefined) {
            queryParameters['datasource'] = requestParameters.datasource;
        }

        if (requestParameters.flag !== undefined) {
            queryParameters['flag'] = requestParameters.flag;
        }

        const headerParameters: runtime.HTTPHeaders = {};

        if (requestParameters.ifNoneMatch !== undefined && requestParameters.ifNoneMatch !== null) {
            headerParameters['If-None-Match'] = String(requestParameters.ifNoneMatch);
        }

        const response = await this.request({
            path: `/route/{origin}/{destination}/`.replace(`{${"destination"}}`, encodeURIComponent(String(requestParameters.destination))).replace(`{${"origin"}}`, encodeURIComponent(String(requestParameters.origin))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse<any>(response);
    }

    /**
     * Get the systems between origin and destination  --- Alternate route: `/dev/route/{origin}/{destination}/`  Alternate route: `/legacy/route/{origin}/{destination}/`  Alternate route: `/v1/route/{origin}/{destination}/`  --- This route is cached for up to 86400 seconds
     * Get route
     */
    async getRouteOriginDestination(requestParameters: GetRouteOriginDestinationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<number>> {
        const response = await this.getRouteOriginDestinationRaw(requestParameters, initOverrides);
        return await response.value();
    }

}

/**
 * @export
 */
export const GetRouteOriginDestinationDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type GetRouteOriginDestinationDatasourceEnum = typeof GetRouteOriginDestinationDatasourceEnum[keyof typeof GetRouteOriginDestinationDatasourceEnum];
/**
 * @export
 */
export const GetRouteOriginDestinationFlagEnum = {
    Shortest: 'shortest',
    Secure: 'secure',
    Insecure: 'insecure'
} as const;
export type GetRouteOriginDestinationFlagEnum = typeof GetRouteOriginDestinationFlagEnum[keyof typeof GetRouteOriginDestinationFlagEnum];
