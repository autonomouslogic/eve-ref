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
 * @interface GetUniverseCategoriesCategoryIdOk
 */
export interface GetUniverseCategoriesCategoryIdOk {
    /**
     * category_id integer
     * @type {number}
     * @memberof GetUniverseCategoriesCategoryIdOk
     */
    categoryId: number;
    /**
     * groups array
     * @type {Array<number>}
     * @memberof GetUniverseCategoriesCategoryIdOk
     */
    groups: Array<number>;
    /**
     * name string
     * @type {string}
     * @memberof GetUniverseCategoriesCategoryIdOk
     */
    name: string;
    /**
     * published boolean
     * @type {boolean}
     * @memberof GetUniverseCategoriesCategoryIdOk
     */
    published: boolean;
}

/**
 * Check if a given object implements the GetUniverseCategoriesCategoryIdOk interface.
 */
export function instanceOfGetUniverseCategoriesCategoryIdOk(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "categoryId" in value;
    isInstance = isInstance && "groups" in value;
    isInstance = isInstance && "name" in value;
    isInstance = isInstance && "published" in value;

    return isInstance;
}

export function GetUniverseCategoriesCategoryIdOkFromJSON(json: any): GetUniverseCategoriesCategoryIdOk {
    return GetUniverseCategoriesCategoryIdOkFromJSONTyped(json, false);
}

export function GetUniverseCategoriesCategoryIdOkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetUniverseCategoriesCategoryIdOk {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'categoryId': json['category_id'],
        'groups': json['groups'],
        'name': json['name'],
        'published': json['published'],
    };
}

export function GetUniverseCategoriesCategoryIdOkToJSON(value?: GetUniverseCategoriesCategoryIdOk | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'category_id': value.categoryId,
        'groups': value.groups,
        'name': value.name,
        'published': value.published,
    };
}

