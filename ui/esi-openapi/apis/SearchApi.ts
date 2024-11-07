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
  GetCharactersCharacterIdSearchOk,
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
    GetCharactersCharacterIdSearchOkFromJSON,
    GetCharactersCharacterIdSearchOkToJSON,
    InternalServerErrorFromJSON,
    InternalServerErrorToJSON,
    ServiceUnavailableFromJSON,
    ServiceUnavailableToJSON,
    UnauthorizedFromJSON,
    UnauthorizedToJSON,
} from '../models';

export interface GetCharactersCharacterIdSearchRequest {
    categories: Set<GetCharactersCharacterIdSearchCategoriesEnum>;
    characterId: number;
    search: string;
    acceptLanguage?: GetCharactersCharacterIdSearchAcceptLanguageEnum;
    datasource?: GetCharactersCharacterIdSearchDatasourceEnum;
    ifNoneMatch?: string;
    language?: GetCharactersCharacterIdSearchLanguageEnum;
    strict?: boolean;
    token?: string;
}

/**
 * 
 */
export class SearchApi extends runtime.BaseAPI {

    /**
     * Search for entities that match a given sub-string.  --- Alternate route: `/dev/characters/{character_id}/search/`  Alternate route: `/legacy/characters/{character_id}/search/`  Alternate route: `/v3/characters/{character_id}/search/`  --- This route is cached for up to 3600 seconds
     * Search on a string
     */
    async getCharactersCharacterIdSearchRaw(requestParameters: GetCharactersCharacterIdSearchRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<GetCharactersCharacterIdSearchOk>> {
        if (requestParameters.categories === null || requestParameters.categories === undefined) {
            throw new runtime.RequiredError('categories','Required parameter requestParameters.categories was null or undefined when calling getCharactersCharacterIdSearch.');
        }

        if (requestParameters.characterId === null || requestParameters.characterId === undefined) {
            throw new runtime.RequiredError('characterId','Required parameter requestParameters.characterId was null or undefined when calling getCharactersCharacterIdSearch.');
        }

        if (requestParameters.search === null || requestParameters.search === undefined) {
            throw new runtime.RequiredError('search','Required parameter requestParameters.search was null or undefined when calling getCharactersCharacterIdSearch.');
        }

        const queryParameters: any = {};

        if (requestParameters.categories) {
            queryParameters['categories'] = Array.from(requestParameters.categories).join(runtime.COLLECTION_FORMATS["csv"]);
        }

        if (requestParameters.datasource !== undefined) {
            queryParameters['datasource'] = requestParameters.datasource;
        }

        if (requestParameters.language !== undefined) {
            queryParameters['language'] = requestParameters.language;
        }

        if (requestParameters.search !== undefined) {
            queryParameters['search'] = requestParameters.search;
        }

        if (requestParameters.strict !== undefined) {
            queryParameters['strict'] = requestParameters.strict;
        }

        if (requestParameters.token !== undefined) {
            queryParameters['token'] = requestParameters.token;
        }

        const headerParameters: runtime.HTTPHeaders = {};

        if (requestParameters.acceptLanguage !== undefined && requestParameters.acceptLanguage !== null) {
            headerParameters['Accept-Language'] = String(requestParameters.acceptLanguage);
        }

        if (requestParameters.ifNoneMatch !== undefined && requestParameters.ifNoneMatch !== null) {
            headerParameters['If-None-Match'] = String(requestParameters.ifNoneMatch);
        }

        if (this.configuration && this.configuration.accessToken) {
            // oauth required
            headerParameters["Authorization"] = await this.configuration.accessToken("evesso", ["esi-search.search_structures.v1"]);
        }

        const response = await this.request({
            path: `/characters/{character_id}/search/`.replace(`{${"character_id"}}`, encodeURIComponent(String(requestParameters.characterId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => GetCharactersCharacterIdSearchOkFromJSON(jsonValue));
    }

    /**
     * Search for entities that match a given sub-string.  --- Alternate route: `/dev/characters/{character_id}/search/`  Alternate route: `/legacy/characters/{character_id}/search/`  Alternate route: `/v3/characters/{character_id}/search/`  --- This route is cached for up to 3600 seconds
     * Search on a string
     */
    async getCharactersCharacterIdSearch(requestParameters: GetCharactersCharacterIdSearchRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<GetCharactersCharacterIdSearchOk> {
        const response = await this.getCharactersCharacterIdSearchRaw(requestParameters, initOverrides);
        return await response.value();
    }

}

/**
 * @export
 */
export const GetCharactersCharacterIdSearchCategoriesEnum = {
    Agent: 'agent',
    Alliance: 'alliance',
    Character: 'character',
    Constellation: 'constellation',
    Corporation: 'corporation',
    Faction: 'faction',
    InventoryType: 'inventory_type',
    Region: 'region',
    SolarSystem: 'solar_system',
    Station: 'station',
    Structure: 'structure'
} as const;
export type GetCharactersCharacterIdSearchCategoriesEnum = typeof GetCharactersCharacterIdSearchCategoriesEnum[keyof typeof GetCharactersCharacterIdSearchCategoriesEnum];
/**
 * @export
 */
export const GetCharactersCharacterIdSearchAcceptLanguageEnum = {
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
export type GetCharactersCharacterIdSearchAcceptLanguageEnum = typeof GetCharactersCharacterIdSearchAcceptLanguageEnum[keyof typeof GetCharactersCharacterIdSearchAcceptLanguageEnum];
/**
 * @export
 */
export const GetCharactersCharacterIdSearchDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type GetCharactersCharacterIdSearchDatasourceEnum = typeof GetCharactersCharacterIdSearchDatasourceEnum[keyof typeof GetCharactersCharacterIdSearchDatasourceEnum];
/**
 * @export
 */
export const GetCharactersCharacterIdSearchLanguageEnum = {
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
export type GetCharactersCharacterIdSearchLanguageEnum = typeof GetCharactersCharacterIdSearchLanguageEnum[keyof typeof GetCharactersCharacterIdSearchLanguageEnum];
