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
 * @interface GetOpportunitiesTasksTaskIdOk
 */
export interface GetOpportunitiesTasksTaskIdOk {
    /**
     * description string
     * @type {string}
     * @memberof GetOpportunitiesTasksTaskIdOk
     */
    description: string;
    /**
     * name string
     * @type {string}
     * @memberof GetOpportunitiesTasksTaskIdOk
     */
    name: string;
    /**
     * notification string
     * @type {string}
     * @memberof GetOpportunitiesTasksTaskIdOk
     */
    notification: string;
    /**
     * task_id integer
     * @type {number}
     * @memberof GetOpportunitiesTasksTaskIdOk
     */
    taskId: number;
}

/**
 * Check if a given object implements the GetOpportunitiesTasksTaskIdOk interface.
 */
export function instanceOfGetOpportunitiesTasksTaskIdOk(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "description" in value;
    isInstance = isInstance && "name" in value;
    isInstance = isInstance && "notification" in value;
    isInstance = isInstance && "taskId" in value;

    return isInstance;
}

export function GetOpportunitiesTasksTaskIdOkFromJSON(json: any): GetOpportunitiesTasksTaskIdOk {
    return GetOpportunitiesTasksTaskIdOkFromJSONTyped(json, false);
}

export function GetOpportunitiesTasksTaskIdOkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetOpportunitiesTasksTaskIdOk {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'description': json['description'],
        'name': json['name'],
        'notification': json['notification'],
        'taskId': json['task_id'],
    };
}

export function GetOpportunitiesTasksTaskIdOkToJSON(value?: GetOpportunitiesTasksTaskIdOk | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'description': value.description,
        'name': value.name,
        'notification': value.notification,
        'task_id': value.taskId,
    };
}

