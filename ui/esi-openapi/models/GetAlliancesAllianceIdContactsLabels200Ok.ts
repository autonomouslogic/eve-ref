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
 * @interface GetAlliancesAllianceIdContactsLabels200Ok
 */
export interface GetAlliancesAllianceIdContactsLabels200Ok {
    /**
     * label_id integer
     * @type {number}
     * @memberof GetAlliancesAllianceIdContactsLabels200Ok
     */
    labelId: number;
    /**
     * label_name string
     * @type {string}
     * @memberof GetAlliancesAllianceIdContactsLabels200Ok
     */
    labelName: string;
}

/**
 * Check if a given object implements the GetAlliancesAllianceIdContactsLabels200Ok interface.
 */
export function instanceOfGetAlliancesAllianceIdContactsLabels200Ok(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "labelId" in value;
    isInstance = isInstance && "labelName" in value;

    return isInstance;
}

export function GetAlliancesAllianceIdContactsLabels200OkFromJSON(json: any): GetAlliancesAllianceIdContactsLabels200Ok {
    return GetAlliancesAllianceIdContactsLabels200OkFromJSONTyped(json, false);
}

export function GetAlliancesAllianceIdContactsLabels200OkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetAlliancesAllianceIdContactsLabels200Ok {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'labelId': json['label_id'],
        'labelName': json['label_name'],
    };
}

export function GetAlliancesAllianceIdContactsLabels200OkToJSON(value?: GetAlliancesAllianceIdContactsLabels200Ok | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'label_id': value.labelId,
        'label_name': value.labelName,
    };
}

