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
import type { GetIndustrySystemsCostIndice } from './GetIndustrySystemsCostIndice';
import {
    GetIndustrySystemsCostIndiceFromJSON,
    GetIndustrySystemsCostIndiceFromJSONTyped,
    GetIndustrySystemsCostIndiceToJSON,
} from './GetIndustrySystemsCostIndice';

/**
 * 200 ok object
 * @export
 * @interface GetIndustrySystems200Ok
 */
export interface GetIndustrySystems200Ok {
    /**
     * cost_indices array
     * @type {Array<GetIndustrySystemsCostIndice>}
     * @memberof GetIndustrySystems200Ok
     */
    costIndices: Array<GetIndustrySystemsCostIndice>;
    /**
     * solar_system_id integer
     * @type {number}
     * @memberof GetIndustrySystems200Ok
     */
    solarSystemId: number;
}

/**
 * Check if a given object implements the GetIndustrySystems200Ok interface.
 */
export function instanceOfGetIndustrySystems200Ok(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "costIndices" in value;
    isInstance = isInstance && "solarSystemId" in value;

    return isInstance;
}

export function GetIndustrySystems200OkFromJSON(json: any): GetIndustrySystems200Ok {
    return GetIndustrySystems200OkFromJSONTyped(json, false);
}

export function GetIndustrySystems200OkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetIndustrySystems200Ok {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'costIndices': ((json['cost_indices'] as Array<any>).map(GetIndustrySystemsCostIndiceFromJSON)),
        'solarSystemId': json['solar_system_id'],
    };
}

export function GetIndustrySystems200OkToJSON(value?: GetIndustrySystems200Ok | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'cost_indices': ((value.costIndices as Array<any>).map(GetIndustrySystemsCostIndiceToJSON)),
        'solar_system_id': value.solarSystemId,
    };
}

