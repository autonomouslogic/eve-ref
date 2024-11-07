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
 * invitation object
 * @export
 * @interface PostFleetsFleetIdMembersInvitation
 */
export interface PostFleetsFleetIdMembersInvitation {
    /**
     * The character you want to invite
     * @type {number}
     * @memberof PostFleetsFleetIdMembersInvitation
     */
    characterId: number;
    /**
     * If a character is invited with the `fleet_commander` role, neither `wing_id` or `squad_id` should be specified. If a character is invited with the `wing_commander` role, only `wing_id` should be specified. If a character is invited with the `squad_commander` role, both `wing_id` and `squad_id` should be specified. If a character is invited with the `squad_member` role, `wing_id` and `squad_id` should either both be specified or not specified at all. If they aren’t specified, the invited character will join any squad with available positions.
     * @type {string}
     * @memberof PostFleetsFleetIdMembersInvitation
     */
    role: PostFleetsFleetIdMembersInvitationRoleEnum;
    /**
     * squad_id integer
     * @type {number}
     * @memberof PostFleetsFleetIdMembersInvitation
     */
    squadId?: number;
    /**
     * wing_id integer
     * @type {number}
     * @memberof PostFleetsFleetIdMembersInvitation
     */
    wingId?: number;
}


/**
 * @export
 */
export const PostFleetsFleetIdMembersInvitationRoleEnum = {
    FleetCommander: 'fleet_commander',
    WingCommander: 'wing_commander',
    SquadCommander: 'squad_commander',
    SquadMember: 'squad_member'
} as const;
export type PostFleetsFleetIdMembersInvitationRoleEnum = typeof PostFleetsFleetIdMembersInvitationRoleEnum[keyof typeof PostFleetsFleetIdMembersInvitationRoleEnum];


/**
 * Check if a given object implements the PostFleetsFleetIdMembersInvitation interface.
 */
export function instanceOfPostFleetsFleetIdMembersInvitation(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "characterId" in value;
    isInstance = isInstance && "role" in value;

    return isInstance;
}

export function PostFleetsFleetIdMembersInvitationFromJSON(json: any): PostFleetsFleetIdMembersInvitation {
    return PostFleetsFleetIdMembersInvitationFromJSONTyped(json, false);
}

export function PostFleetsFleetIdMembersInvitationFromJSONTyped(json: any, ignoreDiscriminator: boolean): PostFleetsFleetIdMembersInvitation {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'characterId': json['character_id'],
        'role': json['role'],
        'squadId': !exists(json, 'squad_id') ? undefined : json['squad_id'],
        'wingId': !exists(json, 'wing_id') ? undefined : json['wing_id'],
    };
}

export function PostFleetsFleetIdMembersInvitationToJSON(value?: PostFleetsFleetIdMembersInvitation | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'character_id': value.characterId,
        'role': value.role,
        'squad_id': value.squadId,
        'wing_id': value.wingId,
    };
}

