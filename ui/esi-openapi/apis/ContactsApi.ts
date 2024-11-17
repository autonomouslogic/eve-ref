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
  GetAlliancesAllianceIdContacts200Ok,
  GetAlliancesAllianceIdContactsLabels200Ok,
  GetCharactersCharacterIdContacts200Ok,
  GetCharactersCharacterIdContactsLabels200Ok,
  GetCorporationsCorporationIdContacts200Ok,
  GetCorporationsCorporationIdContactsLabels200Ok,
  InternalServerError,
  PostCharactersCharacterIdContactsError520,
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
    GetAlliancesAllianceIdContacts200OkFromJSON,
    GetAlliancesAllianceIdContacts200OkToJSON,
    GetAlliancesAllianceIdContactsLabels200OkFromJSON,
    GetAlliancesAllianceIdContactsLabels200OkToJSON,
    GetCharactersCharacterIdContacts200OkFromJSON,
    GetCharactersCharacterIdContacts200OkToJSON,
    GetCharactersCharacterIdContactsLabels200OkFromJSON,
    GetCharactersCharacterIdContactsLabels200OkToJSON,
    GetCorporationsCorporationIdContacts200OkFromJSON,
    GetCorporationsCorporationIdContacts200OkToJSON,
    GetCorporationsCorporationIdContactsLabels200OkFromJSON,
    GetCorporationsCorporationIdContactsLabels200OkToJSON,
    InternalServerErrorFromJSON,
    InternalServerErrorToJSON,
    PostCharactersCharacterIdContactsError520FromJSON,
    PostCharactersCharacterIdContactsError520ToJSON,
    ServiceUnavailableFromJSON,
    ServiceUnavailableToJSON,
    UnauthorizedFromJSON,
    UnauthorizedToJSON,
} from '../models';

export interface DeleteCharactersCharacterIdContactsRequest {
    characterId: number;
    contactIds: Array<number>;
    datasource?: DeleteCharactersCharacterIdContactsDatasourceEnum;
    token?: string;
}

export interface GetAlliancesAllianceIdContactsRequest {
    allianceId: number;
    datasource?: GetAlliancesAllianceIdContactsDatasourceEnum;
    ifNoneMatch?: string;
    page?: number;
    token?: string;
}

export interface GetAlliancesAllianceIdContactsLabelsRequest {
    allianceId: number;
    datasource?: GetAlliancesAllianceIdContactsLabelsDatasourceEnum;
    ifNoneMatch?: string;
    token?: string;
}

export interface GetCharactersCharacterIdContactsRequest {
    characterId: number;
    datasource?: GetCharactersCharacterIdContactsDatasourceEnum;
    ifNoneMatch?: string;
    page?: number;
    token?: string;
}

export interface GetCharactersCharacterIdContactsLabelsRequest {
    characterId: number;
    datasource?: GetCharactersCharacterIdContactsLabelsDatasourceEnum;
    ifNoneMatch?: string;
    token?: string;
}

export interface GetCorporationsCorporationIdContactsRequest {
    corporationId: number;
    datasource?: GetCorporationsCorporationIdContactsDatasourceEnum;
    ifNoneMatch?: string;
    page?: number;
    token?: string;
}

export interface GetCorporationsCorporationIdContactsLabelsRequest {
    corporationId: number;
    datasource?: GetCorporationsCorporationIdContactsLabelsDatasourceEnum;
    ifNoneMatch?: string;
    token?: string;
}

export interface PostCharactersCharacterIdContactsRequest {
    characterId: number;
    standing: number;
    contactIds: Array<number>;
    datasource?: PostCharactersCharacterIdContactsDatasourceEnum;
    labelIds?: Array<number>;
    token?: string;
    watched?: boolean;
}

export interface PutCharactersCharacterIdContactsRequest {
    characterId: number;
    standing: number;
    contactIds: Array<number>;
    datasource?: PutCharactersCharacterIdContactsDatasourceEnum;
    labelIds?: Array<number>;
    token?: string;
    watched?: boolean;
}

/**
 * 
 */
export class ContactsApi extends runtime.BaseAPI {

    /**
     * Bulk delete contacts  --- Alternate route: `/dev/characters/{character_id}/contacts/`  Alternate route: `/v2/characters/{character_id}/contacts/` 
     * Delete contacts
     */
    async deleteCharactersCharacterIdContactsRaw(requestParameters: DeleteCharactersCharacterIdContactsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<void>> {
        if (requestParameters.characterId === null || requestParameters.characterId === undefined) {
            throw new runtime.RequiredError('characterId','Required parameter requestParameters.characterId was null or undefined when calling deleteCharactersCharacterIdContacts.');
        }

        if (requestParameters.contactIds === null || requestParameters.contactIds === undefined) {
            throw new runtime.RequiredError('contactIds','Required parameter requestParameters.contactIds was null or undefined when calling deleteCharactersCharacterIdContacts.');
        }

        const queryParameters: any = {};

        if (requestParameters.contactIds) {
            queryParameters['contact_ids'] = requestParameters.contactIds.join(runtime.COLLECTION_FORMATS["csv"]);
        }

        if (requestParameters.datasource !== undefined) {
            queryParameters['datasource'] = requestParameters.datasource;
        }

        if (requestParameters.token !== undefined) {
            queryParameters['token'] = requestParameters.token;
        }

        const headerParameters: runtime.HTTPHeaders = {};

        if (this.configuration && this.configuration.accessToken) {
            // oauth required
            headerParameters["Authorization"] = await this.configuration.accessToken("evesso", ["esi-characters.write_contacts.v1"]);
        }

        const response = await this.request({
            path: `/characters/{character_id}/contacts/`.replace(`{${"character_id"}}`, encodeURIComponent(String(requestParameters.characterId))),
            method: 'DELETE',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.VoidApiResponse(response);
    }

    /**
     * Bulk delete contacts  --- Alternate route: `/dev/characters/{character_id}/contacts/`  Alternate route: `/v2/characters/{character_id}/contacts/` 
     * Delete contacts
     */
    async deleteCharactersCharacterIdContacts(requestParameters: DeleteCharactersCharacterIdContactsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<void> {
        await this.deleteCharactersCharacterIdContactsRaw(requestParameters, initOverrides);
    }

    /**
     * Return contacts of an alliance  --- Alternate route: `/dev/alliances/{alliance_id}/contacts/`  Alternate route: `/v2/alliances/{alliance_id}/contacts/`  --- This route is cached for up to 300 seconds
     * Get alliance contacts
     */
    async getAlliancesAllianceIdContactsRaw(requestParameters: GetAlliancesAllianceIdContactsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<GetAlliancesAllianceIdContacts200Ok>>> {
        if (requestParameters.allianceId === null || requestParameters.allianceId === undefined) {
            throw new runtime.RequiredError('allianceId','Required parameter requestParameters.allianceId was null or undefined when calling getAlliancesAllianceIdContacts.');
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
            headerParameters["Authorization"] = await this.configuration.accessToken("evesso", ["esi-alliances.read_contacts.v1"]);
        }

        const response = await this.request({
            path: `/alliances/{alliance_id}/contacts/`.replace(`{${"alliance_id"}}`, encodeURIComponent(String(requestParameters.allianceId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(GetAlliancesAllianceIdContacts200OkFromJSON));
    }

    /**
     * Return contacts of an alliance  --- Alternate route: `/dev/alliances/{alliance_id}/contacts/`  Alternate route: `/v2/alliances/{alliance_id}/contacts/`  --- This route is cached for up to 300 seconds
     * Get alliance contacts
     */
    async getAlliancesAllianceIdContacts(requestParameters: GetAlliancesAllianceIdContactsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<GetAlliancesAllianceIdContacts200Ok>> {
        const response = await this.getAlliancesAllianceIdContactsRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Return custom labels for an alliance\'s contacts  --- Alternate route: `/dev/alliances/{alliance_id}/contacts/labels/`  Alternate route: `/legacy/alliances/{alliance_id}/contacts/labels/`  Alternate route: `/v1/alliances/{alliance_id}/contacts/labels/`  --- This route is cached for up to 300 seconds
     * Get alliance contact labels
     */
    async getAlliancesAllianceIdContactsLabelsRaw(requestParameters: GetAlliancesAllianceIdContactsLabelsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<GetAlliancesAllianceIdContactsLabels200Ok>>> {
        if (requestParameters.allianceId === null || requestParameters.allianceId === undefined) {
            throw new runtime.RequiredError('allianceId','Required parameter requestParameters.allianceId was null or undefined when calling getAlliancesAllianceIdContactsLabels.');
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
            headerParameters["Authorization"] = await this.configuration.accessToken("evesso", ["esi-alliances.read_contacts.v1"]);
        }

        const response = await this.request({
            path: `/alliances/{alliance_id}/contacts/labels/`.replace(`{${"alliance_id"}}`, encodeURIComponent(String(requestParameters.allianceId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(GetAlliancesAllianceIdContactsLabels200OkFromJSON));
    }

    /**
     * Return custom labels for an alliance\'s contacts  --- Alternate route: `/dev/alliances/{alliance_id}/contacts/labels/`  Alternate route: `/legacy/alliances/{alliance_id}/contacts/labels/`  Alternate route: `/v1/alliances/{alliance_id}/contacts/labels/`  --- This route is cached for up to 300 seconds
     * Get alliance contact labels
     */
    async getAlliancesAllianceIdContactsLabels(requestParameters: GetAlliancesAllianceIdContactsLabelsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<GetAlliancesAllianceIdContactsLabels200Ok>> {
        const response = await this.getAlliancesAllianceIdContactsLabelsRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Return contacts of a character  --- Alternate route: `/dev/characters/{character_id}/contacts/`  Alternate route: `/v2/characters/{character_id}/contacts/`  --- This route is cached for up to 300 seconds
     * Get contacts
     */
    async getCharactersCharacterIdContactsRaw(requestParameters: GetCharactersCharacterIdContactsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<GetCharactersCharacterIdContacts200Ok>>> {
        if (requestParameters.characterId === null || requestParameters.characterId === undefined) {
            throw new runtime.RequiredError('characterId','Required parameter requestParameters.characterId was null or undefined when calling getCharactersCharacterIdContacts.');
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
            headerParameters["Authorization"] = await this.configuration.accessToken("evesso", ["esi-characters.read_contacts.v1"]);
        }

        const response = await this.request({
            path: `/characters/{character_id}/contacts/`.replace(`{${"character_id"}}`, encodeURIComponent(String(requestParameters.characterId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(GetCharactersCharacterIdContacts200OkFromJSON));
    }

    /**
     * Return contacts of a character  --- Alternate route: `/dev/characters/{character_id}/contacts/`  Alternate route: `/v2/characters/{character_id}/contacts/`  --- This route is cached for up to 300 seconds
     * Get contacts
     */
    async getCharactersCharacterIdContacts(requestParameters: GetCharactersCharacterIdContactsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<GetCharactersCharacterIdContacts200Ok>> {
        const response = await this.getCharactersCharacterIdContactsRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Return custom labels for a character\'s contacts  --- Alternate route: `/dev/characters/{character_id}/contacts/labels/`  Alternate route: `/legacy/characters/{character_id}/contacts/labels/`  Alternate route: `/v1/characters/{character_id}/contacts/labels/`  --- This route is cached for up to 300 seconds
     * Get contact labels
     */
    async getCharactersCharacterIdContactsLabelsRaw(requestParameters: GetCharactersCharacterIdContactsLabelsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<GetCharactersCharacterIdContactsLabels200Ok>>> {
        if (requestParameters.characterId === null || requestParameters.characterId === undefined) {
            throw new runtime.RequiredError('characterId','Required parameter requestParameters.characterId was null or undefined when calling getCharactersCharacterIdContactsLabels.');
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
            headerParameters["Authorization"] = await this.configuration.accessToken("evesso", ["esi-characters.read_contacts.v1"]);
        }

        const response = await this.request({
            path: `/characters/{character_id}/contacts/labels/`.replace(`{${"character_id"}}`, encodeURIComponent(String(requestParameters.characterId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(GetCharactersCharacterIdContactsLabels200OkFromJSON));
    }

    /**
     * Return custom labels for a character\'s contacts  --- Alternate route: `/dev/characters/{character_id}/contacts/labels/`  Alternate route: `/legacy/characters/{character_id}/contacts/labels/`  Alternate route: `/v1/characters/{character_id}/contacts/labels/`  --- This route is cached for up to 300 seconds
     * Get contact labels
     */
    async getCharactersCharacterIdContactsLabels(requestParameters: GetCharactersCharacterIdContactsLabelsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<GetCharactersCharacterIdContactsLabels200Ok>> {
        const response = await this.getCharactersCharacterIdContactsLabelsRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Return contacts of a corporation  --- Alternate route: `/dev/corporations/{corporation_id}/contacts/`  Alternate route: `/v2/corporations/{corporation_id}/contacts/`  --- This route is cached for up to 300 seconds
     * Get corporation contacts
     */
    async getCorporationsCorporationIdContactsRaw(requestParameters: GetCorporationsCorporationIdContactsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<GetCorporationsCorporationIdContacts200Ok>>> {
        if (requestParameters.corporationId === null || requestParameters.corporationId === undefined) {
            throw new runtime.RequiredError('corporationId','Required parameter requestParameters.corporationId was null or undefined when calling getCorporationsCorporationIdContacts.');
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
            headerParameters["Authorization"] = await this.configuration.accessToken("evesso", ["esi-corporations.read_contacts.v1"]);
        }

        const response = await this.request({
            path: `/corporations/{corporation_id}/contacts/`.replace(`{${"corporation_id"}}`, encodeURIComponent(String(requestParameters.corporationId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(GetCorporationsCorporationIdContacts200OkFromJSON));
    }

    /**
     * Return contacts of a corporation  --- Alternate route: `/dev/corporations/{corporation_id}/contacts/`  Alternate route: `/v2/corporations/{corporation_id}/contacts/`  --- This route is cached for up to 300 seconds
     * Get corporation contacts
     */
    async getCorporationsCorporationIdContacts(requestParameters: GetCorporationsCorporationIdContactsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<GetCorporationsCorporationIdContacts200Ok>> {
        const response = await this.getCorporationsCorporationIdContactsRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Return custom labels for a corporation\'s contacts  --- Alternate route: `/dev/corporations/{corporation_id}/contacts/labels/`  Alternate route: `/legacy/corporations/{corporation_id}/contacts/labels/`  Alternate route: `/v1/corporations/{corporation_id}/contacts/labels/`  --- This route is cached for up to 300 seconds
     * Get corporation contact labels
     */
    async getCorporationsCorporationIdContactsLabelsRaw(requestParameters: GetCorporationsCorporationIdContactsLabelsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<GetCorporationsCorporationIdContactsLabels200Ok>>> {
        if (requestParameters.corporationId === null || requestParameters.corporationId === undefined) {
            throw new runtime.RequiredError('corporationId','Required parameter requestParameters.corporationId was null or undefined when calling getCorporationsCorporationIdContactsLabels.');
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
            headerParameters["Authorization"] = await this.configuration.accessToken("evesso", ["esi-corporations.read_contacts.v1"]);
        }

        const response = await this.request({
            path: `/corporations/{corporation_id}/contacts/labels/`.replace(`{${"corporation_id"}}`, encodeURIComponent(String(requestParameters.corporationId))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(GetCorporationsCorporationIdContactsLabels200OkFromJSON));
    }

    /**
     * Return custom labels for a corporation\'s contacts  --- Alternate route: `/dev/corporations/{corporation_id}/contacts/labels/`  Alternate route: `/legacy/corporations/{corporation_id}/contacts/labels/`  Alternate route: `/v1/corporations/{corporation_id}/contacts/labels/`  --- This route is cached for up to 300 seconds
     * Get corporation contact labels
     */
    async getCorporationsCorporationIdContactsLabels(requestParameters: GetCorporationsCorporationIdContactsLabelsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<GetCorporationsCorporationIdContactsLabels200Ok>> {
        const response = await this.getCorporationsCorporationIdContactsLabelsRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Bulk add contacts with same settings  --- Alternate route: `/dev/characters/{character_id}/contacts/`  Alternate route: `/v2/characters/{character_id}/contacts/` 
     * Add contacts
     */
    async postCharactersCharacterIdContactsRaw(requestParameters: PostCharactersCharacterIdContactsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<number>>> {
        if (requestParameters.characterId === null || requestParameters.characterId === undefined) {
            throw new runtime.RequiredError('characterId','Required parameter requestParameters.characterId was null or undefined when calling postCharactersCharacterIdContacts.');
        }

        if (requestParameters.standing === null || requestParameters.standing === undefined) {
            throw new runtime.RequiredError('standing','Required parameter requestParameters.standing was null or undefined when calling postCharactersCharacterIdContacts.');
        }

        if (requestParameters.contactIds === null || requestParameters.contactIds === undefined) {
            throw new runtime.RequiredError('contactIds','Required parameter requestParameters.contactIds was null or undefined when calling postCharactersCharacterIdContacts.');
        }

        const queryParameters: any = {};

        if (requestParameters.datasource !== undefined) {
            queryParameters['datasource'] = requestParameters.datasource;
        }

        if (requestParameters.labelIds) {
            queryParameters['label_ids'] = requestParameters.labelIds.join(runtime.COLLECTION_FORMATS["csv"]);
        }

        if (requestParameters.standing !== undefined) {
            queryParameters['standing'] = requestParameters.standing;
        }

        if (requestParameters.token !== undefined) {
            queryParameters['token'] = requestParameters.token;
        }

        if (requestParameters.watched !== undefined) {
            queryParameters['watched'] = requestParameters.watched;
        }

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        if (this.configuration && this.configuration.accessToken) {
            // oauth required
            headerParameters["Authorization"] = await this.configuration.accessToken("evesso", ["esi-characters.write_contacts.v1"]);
        }

        const response = await this.request({
            path: `/characters/{character_id}/contacts/`.replace(`{${"character_id"}}`, encodeURIComponent(String(requestParameters.characterId))),
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
            body: requestParameters.contactIds,
        }, initOverrides);

        return new runtime.JSONApiResponse<any>(response);
    }

    /**
     * Bulk add contacts with same settings  --- Alternate route: `/dev/characters/{character_id}/contacts/`  Alternate route: `/v2/characters/{character_id}/contacts/` 
     * Add contacts
     */
    async postCharactersCharacterIdContacts(requestParameters: PostCharactersCharacterIdContactsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<number>> {
        const response = await this.postCharactersCharacterIdContactsRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Bulk edit contacts with same settings  --- Alternate route: `/dev/characters/{character_id}/contacts/`  Alternate route: `/v2/characters/{character_id}/contacts/` 
     * Edit contacts
     */
    async putCharactersCharacterIdContactsRaw(requestParameters: PutCharactersCharacterIdContactsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<void>> {
        if (requestParameters.characterId === null || requestParameters.characterId === undefined) {
            throw new runtime.RequiredError('characterId','Required parameter requestParameters.characterId was null or undefined when calling putCharactersCharacterIdContacts.');
        }

        if (requestParameters.standing === null || requestParameters.standing === undefined) {
            throw new runtime.RequiredError('standing','Required parameter requestParameters.standing was null or undefined when calling putCharactersCharacterIdContacts.');
        }

        if (requestParameters.contactIds === null || requestParameters.contactIds === undefined) {
            throw new runtime.RequiredError('contactIds','Required parameter requestParameters.contactIds was null or undefined when calling putCharactersCharacterIdContacts.');
        }

        const queryParameters: any = {};

        if (requestParameters.datasource !== undefined) {
            queryParameters['datasource'] = requestParameters.datasource;
        }

        if (requestParameters.labelIds) {
            queryParameters['label_ids'] = requestParameters.labelIds.join(runtime.COLLECTION_FORMATS["csv"]);
        }

        if (requestParameters.standing !== undefined) {
            queryParameters['standing'] = requestParameters.standing;
        }

        if (requestParameters.token !== undefined) {
            queryParameters['token'] = requestParameters.token;
        }

        if (requestParameters.watched !== undefined) {
            queryParameters['watched'] = requestParameters.watched;
        }

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        if (this.configuration && this.configuration.accessToken) {
            // oauth required
            headerParameters["Authorization"] = await this.configuration.accessToken("evesso", ["esi-characters.write_contacts.v1"]);
        }

        const response = await this.request({
            path: `/characters/{character_id}/contacts/`.replace(`{${"character_id"}}`, encodeURIComponent(String(requestParameters.characterId))),
            method: 'PUT',
            headers: headerParameters,
            query: queryParameters,
            body: requestParameters.contactIds,
        }, initOverrides);

        return new runtime.VoidApiResponse(response);
    }

    /**
     * Bulk edit contacts with same settings  --- Alternate route: `/dev/characters/{character_id}/contacts/`  Alternate route: `/v2/characters/{character_id}/contacts/` 
     * Edit contacts
     */
    async putCharactersCharacterIdContacts(requestParameters: PutCharactersCharacterIdContactsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<void> {
        await this.putCharactersCharacterIdContactsRaw(requestParameters, initOverrides);
    }

}

/**
 * @export
 */
export const DeleteCharactersCharacterIdContactsDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type DeleteCharactersCharacterIdContactsDatasourceEnum = typeof DeleteCharactersCharacterIdContactsDatasourceEnum[keyof typeof DeleteCharactersCharacterIdContactsDatasourceEnum];
/**
 * @export
 */
export const GetAlliancesAllianceIdContactsDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type GetAlliancesAllianceIdContactsDatasourceEnum = typeof GetAlliancesAllianceIdContactsDatasourceEnum[keyof typeof GetAlliancesAllianceIdContactsDatasourceEnum];
/**
 * @export
 */
export const GetAlliancesAllianceIdContactsLabelsDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type GetAlliancesAllianceIdContactsLabelsDatasourceEnum = typeof GetAlliancesAllianceIdContactsLabelsDatasourceEnum[keyof typeof GetAlliancesAllianceIdContactsLabelsDatasourceEnum];
/**
 * @export
 */
export const GetCharactersCharacterIdContactsDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type GetCharactersCharacterIdContactsDatasourceEnum = typeof GetCharactersCharacterIdContactsDatasourceEnum[keyof typeof GetCharactersCharacterIdContactsDatasourceEnum];
/**
 * @export
 */
export const GetCharactersCharacterIdContactsLabelsDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type GetCharactersCharacterIdContactsLabelsDatasourceEnum = typeof GetCharactersCharacterIdContactsLabelsDatasourceEnum[keyof typeof GetCharactersCharacterIdContactsLabelsDatasourceEnum];
/**
 * @export
 */
export const GetCorporationsCorporationIdContactsDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type GetCorporationsCorporationIdContactsDatasourceEnum = typeof GetCorporationsCorporationIdContactsDatasourceEnum[keyof typeof GetCorporationsCorporationIdContactsDatasourceEnum];
/**
 * @export
 */
export const GetCorporationsCorporationIdContactsLabelsDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type GetCorporationsCorporationIdContactsLabelsDatasourceEnum = typeof GetCorporationsCorporationIdContactsLabelsDatasourceEnum[keyof typeof GetCorporationsCorporationIdContactsLabelsDatasourceEnum];
/**
 * @export
 */
export const PostCharactersCharacterIdContactsDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type PostCharactersCharacterIdContactsDatasourceEnum = typeof PostCharactersCharacterIdContactsDatasourceEnum[keyof typeof PostCharactersCharacterIdContactsDatasourceEnum];
/**
 * @export
 */
export const PutCharactersCharacterIdContactsDatasourceEnum = {
    Tranquility: 'tranquility'
} as const;
export type PutCharactersCharacterIdContactsDatasourceEnum = typeof PutCharactersCharacterIdContactsDatasourceEnum[keyof typeof PutCharactersCharacterIdContactsDatasourceEnum];
