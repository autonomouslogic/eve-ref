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
  GetStatusOk,
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
    GetStatusOkFromJSON,
    GetStatusOkToJSON,
    InternalServerErrorFromJSON,
    InternalServerErrorToJSON,
    ServiceUnavailableFromJSON,
    ServiceUnavailableToJSON,
} from '../models';

export interface GetStatusRequest {
    datasource?: GetStatusDatasourceEnum;
    ifNoneMatch?: string;
}

/**
 * 
 */
export class StatusApi extends runtime.BaseAPI {

    /**
     * EVE Server status  --- Alternate route: `/dev/status/`  Alternate route: `/legacy/status/`  Alternate route: `/v1/status/`  Alternate route: `/v2/status/`  --- This route is cached for up to 30 seconds
     * Retrieve the uptime and player counts
     */
    async getStatusRaw(requestParameters: GetStatusRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<GetStatusOk>> {
        const queryParameters: any = {};

        if (requestParameters.datasource !== undefined) {
            queryParameters['datasource'] = requestParameters.datasource;
        }

        const headerParameters: runtime.HTTPHeaders = {};

        if (requestParameters.ifNoneMatch !== undefined && requestParameters.ifNoneMatch !== null) {
            headerParameters['If-None-Match'] = String(requestParameters.ifNoneMatch);
        }

        const response = await this.request({
            path: `/status/`,
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => GetStatusOkFromJSON(jsonValue));
    }

    /**
     * EVE Server status  --- Alternate route: `/dev/status/`  Alternate route: `/legacy/status/`  Alternate route: `/v1/status/`  Alternate route: `/v2/status/`  --- This route is cached for up to 30 seconds
     * Retrieve the uptime and player counts
     */
    async getStatus(requestParameters: GetStatusRequest = {}, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<GetStatusOk> {
        const response = await this.getStatusRaw(requestParameters, initOverrides);
        return await response.value();
    }

}

/**
 * @export
 */
export const GetStatusDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type GetStatusDatasourceEnum = typeof GetStatusDatasourceEnum[keyof typeof GetStatusDatasourceEnum];