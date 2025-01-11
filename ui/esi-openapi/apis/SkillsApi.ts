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
  GetCharactersCharacterIdAttributesOk,
  GetCharactersCharacterIdSkillqueue200Ok,
  GetCharactersCharacterIdSkillsOk,
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
    GetCharactersCharacterIdAttributesOkFromJSON,
    GetCharactersCharacterIdAttributesOkToJSON,
    GetCharactersCharacterIdSkillqueue200OkFromJSON,
    GetCharactersCharacterIdSkillqueue200OkToJSON,
    GetCharactersCharacterIdSkillsOkFromJSON,
    GetCharactersCharacterIdSkillsOkToJSON,
    InternalServerErrorFromJSON,
    InternalServerErrorToJSON,
    ServiceUnavailableFromJSON,
    ServiceUnavailableToJSON,
    UnauthorizedFromJSON,
    UnauthorizedToJSON,
} from '../models';

export interface GetCharactersCharacterIdAttributesRequest {
    characterId: number;
    datasource?: GetCharactersCharacterIdAttributesDatasourceEnum;
    ifNoneMatch?: string;
    token?: string;
}

export interface GetCharactersCharacterIdSkillqueueRequest {
    characterId: number;
    datasource?: GetCharactersCharacterIdSkillqueueDatasourceEnum;
    ifNoneMatch?: string;
    token?: string;
}

export interface GetCharactersCharacterIdSkillsRequest {
    characterId: number;
    datasource?: GetCharactersCharacterIdSkillsDatasourceEnum;
    ifNoneMatch?: string;
    token?: string;
}

/**
 * 
 */
export class SkillsApi extends runtime.BaseAPI {

    /**
     * Return attributes of a character  --- Alternate route: `/dev/characters/{character_id}/attributes/`  Alternate route: `/legacy/characters/{character_id}/attributes/`  Alternate route: `/v1/characters/{character_id}/attributes/`  --- This route is cached for up to 120 seconds
     * Get character attributes
     */
    async getCharactersCharacterIdAttributesRaw(requestParameters: GetCharactersCharacterIdAttributesRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<GetCharactersCharacterIdAttributesOk>> {
        if (requestParameters.characterId === null || requestParameters.characterId === undefined) {
            throw new runtime.RequiredError('characterId','Required parameter requestParameters.characterId was null or undefined when calling getCharactersCharacterIdAttributes.');
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
            headerParameters["Authorization"] = await this.configuration.accessToken("evesso", ["esi-skills.read_skills.v1"]);
        }

        const response = await this.request({
            path: `/characters/{character_id}/attributes/`.replace(`{${"character_id"}}`, encodeURIComponent(String(requestParameters.characterId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => GetCharactersCharacterIdAttributesOkFromJSON(jsonValue));
    }

    /**
     * Return attributes of a character  --- Alternate route: `/dev/characters/{character_id}/attributes/`  Alternate route: `/legacy/characters/{character_id}/attributes/`  Alternate route: `/v1/characters/{character_id}/attributes/`  --- This route is cached for up to 120 seconds
     * Get character attributes
     */
    async getCharactersCharacterIdAttributes(requestParameters: GetCharactersCharacterIdAttributesRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<GetCharactersCharacterIdAttributesOk> {
        const response = await this.getCharactersCharacterIdAttributesRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * List the configured skill queue for the given character  --- Alternate route: `/dev/characters/{character_id}/skillqueue/`  Alternate route: `/legacy/characters/{character_id}/skillqueue/`  Alternate route: `/v2/characters/{character_id}/skillqueue/`  --- This route is cached for up to 120 seconds
     * Get character\'s skill queue
     */
    async getCharactersCharacterIdSkillqueueRaw(requestParameters: GetCharactersCharacterIdSkillqueueRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<GetCharactersCharacterIdSkillqueue200Ok>>> {
        if (requestParameters.characterId === null || requestParameters.characterId === undefined) {
            throw new runtime.RequiredError('characterId','Required parameter requestParameters.characterId was null or undefined when calling getCharactersCharacterIdSkillqueue.');
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
            headerParameters["Authorization"] = await this.configuration.accessToken("evesso", ["esi-skills.read_skillqueue.v1"]);
        }

        const response = await this.request({
            path: `/characters/{character_id}/skillqueue/`.replace(`{${"character_id"}}`, encodeURIComponent(String(requestParameters.characterId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(GetCharactersCharacterIdSkillqueue200OkFromJSON));
    }

    /**
     * List the configured skill queue for the given character  --- Alternate route: `/dev/characters/{character_id}/skillqueue/`  Alternate route: `/legacy/characters/{character_id}/skillqueue/`  Alternate route: `/v2/characters/{character_id}/skillqueue/`  --- This route is cached for up to 120 seconds
     * Get character\'s skill queue
     */
    async getCharactersCharacterIdSkillqueue(requestParameters: GetCharactersCharacterIdSkillqueueRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<GetCharactersCharacterIdSkillqueue200Ok>> {
        const response = await this.getCharactersCharacterIdSkillqueueRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * List all trained skills for the given character  --- Alternate route: `/dev/characters/{character_id}/skills/`  Alternate route: `/v4/characters/{character_id}/skills/`  --- This route is cached for up to 120 seconds
     * Get character skills
     */
    async getCharactersCharacterIdSkillsRaw(requestParameters: GetCharactersCharacterIdSkillsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<GetCharactersCharacterIdSkillsOk>> {
        if (requestParameters.characterId === null || requestParameters.characterId === undefined) {
            throw new runtime.RequiredError('characterId','Required parameter requestParameters.characterId was null or undefined when calling getCharactersCharacterIdSkills.');
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
            headerParameters["Authorization"] = await this.configuration.accessToken("evesso", ["esi-skills.read_skills.v1"]);
        }

        const response = await this.request({
            path: `/characters/{character_id}/skills/`.replace(`{${"character_id"}}`, encodeURIComponent(String(requestParameters.characterId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => GetCharactersCharacterIdSkillsOkFromJSON(jsonValue));
    }

    /**
     * List all trained skills for the given character  --- Alternate route: `/dev/characters/{character_id}/skills/`  Alternate route: `/v4/characters/{character_id}/skills/`  --- This route is cached for up to 120 seconds
     * Get character skills
     */
    async getCharactersCharacterIdSkills(requestParameters: GetCharactersCharacterIdSkillsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<GetCharactersCharacterIdSkillsOk> {
        const response = await this.getCharactersCharacterIdSkillsRaw(requestParameters, initOverrides);
        return await response.value();
    }

}

/**
 * @export
 */
export const GetCharactersCharacterIdAttributesDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type GetCharactersCharacterIdAttributesDatasourceEnum = typeof GetCharactersCharacterIdAttributesDatasourceEnum[keyof typeof GetCharactersCharacterIdAttributesDatasourceEnum];
/**
 * @export
 */
export const GetCharactersCharacterIdSkillqueueDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type GetCharactersCharacterIdSkillqueueDatasourceEnum = typeof GetCharactersCharacterIdSkillqueueDatasourceEnum[keyof typeof GetCharactersCharacterIdSkillqueueDatasourceEnum];
/**
 * @export
 */
export const GetCharactersCharacterIdSkillsDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type GetCharactersCharacterIdSkillsDatasourceEnum = typeof GetCharactersCharacterIdSkillsDatasourceEnum[keyof typeof GetCharactersCharacterIdSkillsDatasourceEnum];
