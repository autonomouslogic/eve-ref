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
 * @interface GetCharactersCharacterIdSkillqueue200Ok
 */
export interface GetCharactersCharacterIdSkillqueue200Ok {
    /**
     * Date on which training of the skill will complete. Omitted if the skill queue is paused.
     * @type {Date}
     * @memberof GetCharactersCharacterIdSkillqueue200Ok
     */
    finishDate?: Date;
    /**
     * finished_level integer
     * @type {number}
     * @memberof GetCharactersCharacterIdSkillqueue200Ok
     */
    finishedLevel: number;
    /**
     * level_end_sp integer
     * @type {number}
     * @memberof GetCharactersCharacterIdSkillqueue200Ok
     */
    levelEndSp?: number;
    /**
     * Amount of SP that was in the skill when it started training it's current level. Used to calculate % of current level complete.
     * @type {number}
     * @memberof GetCharactersCharacterIdSkillqueue200Ok
     */
    levelStartSp?: number;
    /**
     * queue_position integer
     * @type {number}
     * @memberof GetCharactersCharacterIdSkillqueue200Ok
     */
    queuePosition: number;
    /**
     * skill_id integer
     * @type {number}
     * @memberof GetCharactersCharacterIdSkillqueue200Ok
     */
    skillId: number;
    /**
     * start_date string
     * @type {Date}
     * @memberof GetCharactersCharacterIdSkillqueue200Ok
     */
    startDate?: Date;
    /**
     * training_start_sp integer
     * @type {number}
     * @memberof GetCharactersCharacterIdSkillqueue200Ok
     */
    trainingStartSp?: number;
}

/**
 * Check if a given object implements the GetCharactersCharacterIdSkillqueue200Ok interface.
 */
export function instanceOfGetCharactersCharacterIdSkillqueue200Ok(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "finishedLevel" in value;
    isInstance = isInstance && "queuePosition" in value;
    isInstance = isInstance && "skillId" in value;

    return isInstance;
}

export function GetCharactersCharacterIdSkillqueue200OkFromJSON(json: any): GetCharactersCharacterIdSkillqueue200Ok {
    return GetCharactersCharacterIdSkillqueue200OkFromJSONTyped(json, false);
}

export function GetCharactersCharacterIdSkillqueue200OkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCharactersCharacterIdSkillqueue200Ok {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'finishDate': !exists(json, 'finish_date') ? undefined : (new Date(json['finish_date'])),
        'finishedLevel': json['finished_level'],
        'levelEndSp': !exists(json, 'level_end_sp') ? undefined : json['level_end_sp'],
        'levelStartSp': !exists(json, 'level_start_sp') ? undefined : json['level_start_sp'],
        'queuePosition': json['queue_position'],
        'skillId': json['skill_id'],
        'startDate': !exists(json, 'start_date') ? undefined : (new Date(json['start_date'])),
        'trainingStartSp': !exists(json, 'training_start_sp') ? undefined : json['training_start_sp'],
    };
}

export function GetCharactersCharacterIdSkillqueue200OkToJSON(value?: GetCharactersCharacterIdSkillqueue200Ok | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'finish_date': value.finishDate === undefined ? undefined : (value.finishDate.toISOString()),
        'finished_level': value.finishedLevel,
        'level_end_sp': value.levelEndSp,
        'level_start_sp': value.levelStartSp,
        'queue_position': value.queuePosition,
        'skill_id': value.skillId,
        'start_date': value.startDate === undefined ? undefined : (value.startDate.toISOString()),
        'training_start_sp': value.trainingStartSp,
    };
}

