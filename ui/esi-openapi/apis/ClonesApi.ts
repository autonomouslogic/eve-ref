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


import * as runtime from '../runtime';
import type {
  BadRequest,
  ErrorLimited,
  Forbidden,
  GatewayTimeout,
  GetCharactersCharacterIdClonesOk,
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
    GetCharactersCharacterIdClonesOkFromJSON,
    GetCharactersCharacterIdClonesOkToJSON,
    InternalServerErrorFromJSON,
    InternalServerErrorToJSON,
    ServiceUnavailableFromJSON,
    ServiceUnavailableToJSON,
    UnauthorizedFromJSON,
    UnauthorizedToJSON,
} from '../models';

export interface GetCharactersCharacterIdClonesRequest {
    characterId: number;
    datasource?: GetCharactersCharacterIdClonesDatasourceEnum;
    ifNoneMatch?: string;
    token?: string;
}

export interface GetCharactersCharacterIdImplantsRequest {
    characterId: number;
    datasource?: GetCharactersCharacterIdImplantsDatasourceEnum;
    ifNoneMatch?: string;
    token?: string;
}

/**
 * 
 */
export class ClonesApi extends runtime.BaseAPI {

    /**
     * A list of the character\'s clones  --- Alternate route: `/dev/characters/{character_id}/clones/`  Alternate route: `/v3/characters/{character_id}/clones/`  Alternate route: `/v4/characters/{character_id}/clones/`  --- This route is cached for up to 120 seconds
     * Get clones
     */
    async getCharactersCharacterIdClonesRaw(requestParameters: GetCharactersCharacterIdClonesRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<GetCharactersCharacterIdClonesOk>> {
        if (requestParameters.characterId === null || requestParameters.characterId === undefined) {
            throw new runtime.RequiredError('characterId','Required parameter requestParameters.characterId was null or undefined when calling getCharactersCharacterIdClones.');
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
            headerParameters["Authorization"] = await this.configuration.accessToken("evesso", ["esi-clones.read_clones.v1"]);
        }

        const response = await this.request({
            path: `/characters/{character_id}/clones/`.replace(`{${"character_id"}}`, encodeURIComponent(String(requestParameters.characterId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => GetCharactersCharacterIdClonesOkFromJSON(jsonValue));
    }

    /**
     * A list of the character\'s clones  --- Alternate route: `/dev/characters/{character_id}/clones/`  Alternate route: `/v3/characters/{character_id}/clones/`  Alternate route: `/v4/characters/{character_id}/clones/`  --- This route is cached for up to 120 seconds
     * Get clones
     */
    async getCharactersCharacterIdClones(requestParameters: GetCharactersCharacterIdClonesRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<GetCharactersCharacterIdClonesOk> {
        const response = await this.getCharactersCharacterIdClonesRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Return implants on the active clone of a character  --- Alternate route: `/dev/characters/{character_id}/implants/`  Alternate route: `/legacy/characters/{character_id}/implants/`  Alternate route: `/v1/characters/{character_id}/implants/`  Alternate route: `/v2/characters/{character_id}/implants/`  --- This route is cached for up to 120 seconds
     * Get active implants
     */
    async getCharactersCharacterIdImplantsRaw(requestParameters: GetCharactersCharacterIdImplantsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<number>>> {
        if (requestParameters.characterId === null || requestParameters.characterId === undefined) {
            throw new runtime.RequiredError('characterId','Required parameter requestParameters.characterId was null or undefined when calling getCharactersCharacterIdImplants.');
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
            headerParameters["Authorization"] = await this.configuration.accessToken("evesso", ["esi-clones.read_implants.v1"]);
        }

        const response = await this.request({
            path: `/characters/{character_id}/implants/`.replace(`{${"character_id"}}`, encodeURIComponent(String(requestParameters.characterId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse<any>(response);
    }

    /**
     * Return implants on the active clone of a character  --- Alternate route: `/dev/characters/{character_id}/implants/`  Alternate route: `/legacy/characters/{character_id}/implants/`  Alternate route: `/v1/characters/{character_id}/implants/`  Alternate route: `/v2/characters/{character_id}/implants/`  --- This route is cached for up to 120 seconds
     * Get active implants
     */
    async getCharactersCharacterIdImplants(requestParameters: GetCharactersCharacterIdImplantsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<number>> {
        const response = await this.getCharactersCharacterIdImplantsRaw(requestParameters, initOverrides);
        return await response.value();
    }

}

/**
 * @export
 */
export const GetCharactersCharacterIdClonesDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type GetCharactersCharacterIdClonesDatasourceEnum = typeof GetCharactersCharacterIdClonesDatasourceEnum[keyof typeof GetCharactersCharacterIdClonesDatasourceEnum];
/**
 * @export
 */
export const GetCharactersCharacterIdImplantsDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type GetCharactersCharacterIdImplantsDatasourceEnum = typeof GetCharactersCharacterIdImplantsDatasourceEnum[keyof typeof GetCharactersCharacterIdImplantsDatasourceEnum];
