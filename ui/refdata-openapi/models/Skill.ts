/* tslint:disable */
/* eslint-disable */
/**
 * EVE Ref Reference Data for EVE Online
 * This spec should be considered unstable and subject to change at any time.
 *
 * The version of the OpenAPI document: dev
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { exists, mapValues } from '../runtime';
/**
 * Details about skill types. These are created entirely by EVE Ref.
 * @export
 * @interface Skill
 */
export interface Skill {
    /**
     * 
     * @type {number}
     * @memberof Skill
     */
    typeId?: number;
    /**
     * The dogma attribute ID of the primary training attribute.
     * @type {number}
     * @memberof Skill
     */
    primaryDogmaAttributeId?: number;
    /**
     * The dogma attribute ID of the secondary training attribute.
     * @type {number}
     * @memberof Skill
     */
    secondaryDogmaAttributeId?: number;
    /**
     * The character attribute ID of the primary training attribute.
     * @type {number}
     * @memberof Skill
     */
    primaryCharacterAttributeId?: number;
    /**
     * The character attribute ID of the secondary training attribute.
     * @type {number}
     * @memberof Skill
     */
    secondaryCharacterAttributeId?: number;
    /**
     * 
     * @type {number}
     * @memberof Skill
     */
    trainingTimeMultiplier?: number;
    /**
     * 
     * @type {boolean}
     * @memberof Skill
     */
    canNotBeTrainedOnTrial?: boolean;
    /**
     * The other skills required for this skill.
     * @type {{ [key: string]: number; }}
     * @memberof Skill
     */
    requiredSkills?: { [key: string]: number; };
    /**
     * Which type IDs this skill can be used to reprocess. This is found by cross-referencing dogma attribute reprocessingSkillType [790] on the skills.
     * @type {Array<number>}
     * @memberof Skill
     */
    reprocessableTypeIds?: Array<number>;
}

/**
 * Check if a given object implements the Skill interface.
 */
export function instanceOfSkill(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function SkillFromJSON(json: any): Skill {
    return SkillFromJSONTyped(json, false);
}

export function SkillFromJSONTyped(json: any, ignoreDiscriminator: boolean): Skill {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'typeId': !exists(json, 'type_id') ? undefined : json['type_id'],
        'primaryDogmaAttributeId': !exists(json, 'primary_dogma_attribute_id') ? undefined : json['primary_dogma_attribute_id'],
        'secondaryDogmaAttributeId': !exists(json, 'secondary_dogma_attribute_id') ? undefined : json['secondary_dogma_attribute_id'],
        'primaryCharacterAttributeId': !exists(json, 'primary_character_attribute_id') ? undefined : json['primary_character_attribute_id'],
        'secondaryCharacterAttributeId': !exists(json, 'secondary_character_attribute_id') ? undefined : json['secondary_character_attribute_id'],
        'trainingTimeMultiplier': !exists(json, 'training_time_multiplier') ? undefined : json['training_time_multiplier'],
        'canNotBeTrainedOnTrial': !exists(json, 'can_not_be_trained_on_trial') ? undefined : json['can_not_be_trained_on_trial'],
        'requiredSkills': !exists(json, 'required_skills') ? undefined : json['required_skills'],
        'reprocessableTypeIds': !exists(json, 'reprocessable_type_ids') ? undefined : json['reprocessable_type_ids'],
    };
}

export function SkillToJSON(value?: Skill | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'type_id': value.typeId,
        'primary_dogma_attribute_id': value.primaryDogmaAttributeId,
        'secondary_dogma_attribute_id': value.secondaryDogmaAttributeId,
        'primary_character_attribute_id': value.primaryCharacterAttributeId,
        'secondary_character_attribute_id': value.secondaryCharacterAttributeId,
        'training_time_multiplier': value.trainingTimeMultiplier,
        'can_not_be_trained_on_trial': value.canNotBeTrainedOnTrial,
        'required_skills': value.requiredSkills,
        'reprocessable_type_ids': value.reprocessableTypeIds,
    };
}
