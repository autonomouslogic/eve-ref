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
 * @interface GetOpportunitiesGroupsGroupIdOk
 */
export interface GetOpportunitiesGroupsGroupIdOk {
    /**
     * The groups that are connected to this group on the opportunities map
     * @type {Array<number>}
     * @memberof GetOpportunitiesGroupsGroupIdOk
     */
    connectedGroups: Array<number>;
    /**
     * description string
     * @type {string}
     * @memberof GetOpportunitiesGroupsGroupIdOk
     */
    description: string;
    /**
     * group_id integer
     * @type {number}
     * @memberof GetOpportunitiesGroupsGroupIdOk
     */
    groupId: number;
    /**
     * name string
     * @type {string}
     * @memberof GetOpportunitiesGroupsGroupIdOk
     */
    name: string;
    /**
     * notification string
     * @type {string}
     * @memberof GetOpportunitiesGroupsGroupIdOk
     */
    notification: string;
    /**
     * Tasks need to complete for this group
     * @type {Array<number>}
     * @memberof GetOpportunitiesGroupsGroupIdOk
     */
    requiredTasks: Array<number>;
}

/**
 * Check if a given object implements the GetOpportunitiesGroupsGroupIdOk interface.
 */
export function instanceOfGetOpportunitiesGroupsGroupIdOk(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "connectedGroups" in value;
    isInstance = isInstance && "description" in value;
    isInstance = isInstance && "groupId" in value;
    isInstance = isInstance && "name" in value;
    isInstance = isInstance && "notification" in value;
    isInstance = isInstance && "requiredTasks" in value;

    return isInstance;
}

export function GetOpportunitiesGroupsGroupIdOkFromJSON(json: any): GetOpportunitiesGroupsGroupIdOk {
    return GetOpportunitiesGroupsGroupIdOkFromJSONTyped(json, false);
}

export function GetOpportunitiesGroupsGroupIdOkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetOpportunitiesGroupsGroupIdOk {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'connectedGroups': json['connected_groups'],
        'description': json['description'],
        'groupId': json['group_id'],
        'name': json['name'],
        'notification': json['notification'],
        'requiredTasks': json['required_tasks'],
    };
}

export function GetOpportunitiesGroupsGroupIdOkToJSON(value?: GetOpportunitiesGroupsGroupIdOk | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'connected_groups': value.connectedGroups,
        'description': value.description,
        'group_id': value.groupId,
        'name': value.name,
        'notification': value.notification,
        'required_tasks': value.requiredTasks,
    };
}

