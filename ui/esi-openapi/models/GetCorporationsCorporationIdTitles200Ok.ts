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
 * @interface GetCorporationsCorporationIdTitles200Ok
 */
export interface GetCorporationsCorporationIdTitles200Ok {
    /**
     * grantable_roles array
     * @type {Array<string>}
     * @memberof GetCorporationsCorporationIdTitles200Ok
     */
    grantableRoles?: Array<GetCorporationsCorporationIdTitles200OkGrantableRolesEnum>;
    /**
     * grantable_roles_at_base array
     * @type {Array<string>}
     * @memberof GetCorporationsCorporationIdTitles200Ok
     */
    grantableRolesAtBase?: Array<GetCorporationsCorporationIdTitles200OkGrantableRolesAtBaseEnum>;
    /**
     * grantable_roles_at_hq array
     * @type {Array<string>}
     * @memberof GetCorporationsCorporationIdTitles200Ok
     */
    grantableRolesAtHq?: Array<GetCorporationsCorporationIdTitles200OkGrantableRolesAtHqEnum>;
    /**
     * grantable_roles_at_other array
     * @type {Array<string>}
     * @memberof GetCorporationsCorporationIdTitles200Ok
     */
    grantableRolesAtOther?: Array<GetCorporationsCorporationIdTitles200OkGrantableRolesAtOtherEnum>;
    /**
     * name string
     * @type {string}
     * @memberof GetCorporationsCorporationIdTitles200Ok
     */
    name?: string;
    /**
     * roles array
     * @type {Array<string>}
     * @memberof GetCorporationsCorporationIdTitles200Ok
     */
    roles?: Array<GetCorporationsCorporationIdTitles200OkRolesEnum>;
    /**
     * roles_at_base array
     * @type {Array<string>}
     * @memberof GetCorporationsCorporationIdTitles200Ok
     */
    rolesAtBase?: Array<GetCorporationsCorporationIdTitles200OkRolesAtBaseEnum>;
    /**
     * roles_at_hq array
     * @type {Array<string>}
     * @memberof GetCorporationsCorporationIdTitles200Ok
     */
    rolesAtHq?: Array<GetCorporationsCorporationIdTitles200OkRolesAtHqEnum>;
    /**
     * roles_at_other array
     * @type {Array<string>}
     * @memberof GetCorporationsCorporationIdTitles200Ok
     */
    rolesAtOther?: Array<GetCorporationsCorporationIdTitles200OkRolesAtOtherEnum>;
    /**
     * title_id integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdTitles200Ok
     */
    titleId?: number;
}


/**
 * @export
 */
export const GetCorporationsCorporationIdTitles200OkGrantableRolesEnum = {
    AccountTake1: 'Account_Take_1',
    AccountTake2: 'Account_Take_2',
    AccountTake3: 'Account_Take_3',
    AccountTake4: 'Account_Take_4',
    AccountTake5: 'Account_Take_5',
    AccountTake6: 'Account_Take_6',
    AccountTake7: 'Account_Take_7',
    Accountant: 'Accountant',
    Auditor: 'Auditor',
    CommunicationsOfficer: 'Communications_Officer',
    ConfigEquipment: 'Config_Equipment',
    ConfigStarbaseEquipment: 'Config_Starbase_Equipment',
    ContainerTake1: 'Container_Take_1',
    ContainerTake2: 'Container_Take_2',
    ContainerTake3: 'Container_Take_3',
    ContainerTake4: 'Container_Take_4',
    ContainerTake5: 'Container_Take_5',
    ContainerTake6: 'Container_Take_6',
    ContainerTake7: 'Container_Take_7',
    ContractManager: 'Contract_Manager',
    Diplomat: 'Diplomat',
    Director: 'Director',
    FactoryManager: 'Factory_Manager',
    FittingManager: 'Fitting_Manager',
    HangarQuery1: 'Hangar_Query_1',
    HangarQuery2: 'Hangar_Query_2',
    HangarQuery3: 'Hangar_Query_3',
    HangarQuery4: 'Hangar_Query_4',
    HangarQuery5: 'Hangar_Query_5',
    HangarQuery6: 'Hangar_Query_6',
    HangarQuery7: 'Hangar_Query_7',
    HangarTake1: 'Hangar_Take_1',
    HangarTake2: 'Hangar_Take_2',
    HangarTake3: 'Hangar_Take_3',
    HangarTake4: 'Hangar_Take_4',
    HangarTake5: 'Hangar_Take_5',
    HangarTake6: 'Hangar_Take_6',
    HangarTake7: 'Hangar_Take_7',
    JuniorAccountant: 'Junior_Accountant',
    PersonnelManager: 'Personnel_Manager',
    RentFactoryFacility: 'Rent_Factory_Facility',
    RentOffice: 'Rent_Office',
    RentResearchFacility: 'Rent_Research_Facility',
    SecurityOfficer: 'Security_Officer',
    SkillPlanManager: 'Skill_Plan_Manager',
    StarbaseDefenseOperator: 'Starbase_Defense_Operator',
    StarbaseFuelTechnician: 'Starbase_Fuel_Technician',
    StationManager: 'Station_Manager',
    Trader: 'Trader'
} as const;
export type GetCorporationsCorporationIdTitles200OkGrantableRolesEnum = typeof GetCorporationsCorporationIdTitles200OkGrantableRolesEnum[keyof typeof GetCorporationsCorporationIdTitles200OkGrantableRolesEnum];

/**
 * @export
 */
export const GetCorporationsCorporationIdTitles200OkGrantableRolesAtBaseEnum = {
    AccountTake1: 'Account_Take_1',
    AccountTake2: 'Account_Take_2',
    AccountTake3: 'Account_Take_3',
    AccountTake4: 'Account_Take_4',
    AccountTake5: 'Account_Take_5',
    AccountTake6: 'Account_Take_6',
    AccountTake7: 'Account_Take_7',
    Accountant: 'Accountant',
    Auditor: 'Auditor',
    CommunicationsOfficer: 'Communications_Officer',
    ConfigEquipment: 'Config_Equipment',
    ConfigStarbaseEquipment: 'Config_Starbase_Equipment',
    ContainerTake1: 'Container_Take_1',
    ContainerTake2: 'Container_Take_2',
    ContainerTake3: 'Container_Take_3',
    ContainerTake4: 'Container_Take_4',
    ContainerTake5: 'Container_Take_5',
    ContainerTake6: 'Container_Take_6',
    ContainerTake7: 'Container_Take_7',
    ContractManager: 'Contract_Manager',
    Diplomat: 'Diplomat',
    Director: 'Director',
    FactoryManager: 'Factory_Manager',
    FittingManager: 'Fitting_Manager',
    HangarQuery1: 'Hangar_Query_1',
    HangarQuery2: 'Hangar_Query_2',
    HangarQuery3: 'Hangar_Query_3',
    HangarQuery4: 'Hangar_Query_4',
    HangarQuery5: 'Hangar_Query_5',
    HangarQuery6: 'Hangar_Query_6',
    HangarQuery7: 'Hangar_Query_7',
    HangarTake1: 'Hangar_Take_1',
    HangarTake2: 'Hangar_Take_2',
    HangarTake3: 'Hangar_Take_3',
    HangarTake4: 'Hangar_Take_4',
    HangarTake5: 'Hangar_Take_5',
    HangarTake6: 'Hangar_Take_6',
    HangarTake7: 'Hangar_Take_7',
    JuniorAccountant: 'Junior_Accountant',
    PersonnelManager: 'Personnel_Manager',
    RentFactoryFacility: 'Rent_Factory_Facility',
    RentOffice: 'Rent_Office',
    RentResearchFacility: 'Rent_Research_Facility',
    SecurityOfficer: 'Security_Officer',
    SkillPlanManager: 'Skill_Plan_Manager',
    StarbaseDefenseOperator: 'Starbase_Defense_Operator',
    StarbaseFuelTechnician: 'Starbase_Fuel_Technician',
    StationManager: 'Station_Manager',
    Trader: 'Trader'
} as const;
export type GetCorporationsCorporationIdTitles200OkGrantableRolesAtBaseEnum = typeof GetCorporationsCorporationIdTitles200OkGrantableRolesAtBaseEnum[keyof typeof GetCorporationsCorporationIdTitles200OkGrantableRolesAtBaseEnum];

/**
 * @export
 */
export const GetCorporationsCorporationIdTitles200OkGrantableRolesAtHqEnum = {
    AccountTake1: 'Account_Take_1',
    AccountTake2: 'Account_Take_2',
    AccountTake3: 'Account_Take_3',
    AccountTake4: 'Account_Take_4',
    AccountTake5: 'Account_Take_5',
    AccountTake6: 'Account_Take_6',
    AccountTake7: 'Account_Take_7',
    Accountant: 'Accountant',
    Auditor: 'Auditor',
    CommunicationsOfficer: 'Communications_Officer',
    ConfigEquipment: 'Config_Equipment',
    ConfigStarbaseEquipment: 'Config_Starbase_Equipment',
    ContainerTake1: 'Container_Take_1',
    ContainerTake2: 'Container_Take_2',
    ContainerTake3: 'Container_Take_3',
    ContainerTake4: 'Container_Take_4',
    ContainerTake5: 'Container_Take_5',
    ContainerTake6: 'Container_Take_6',
    ContainerTake7: 'Container_Take_7',
    ContractManager: 'Contract_Manager',
    Diplomat: 'Diplomat',
    Director: 'Director',
    FactoryManager: 'Factory_Manager',
    FittingManager: 'Fitting_Manager',
    HangarQuery1: 'Hangar_Query_1',
    HangarQuery2: 'Hangar_Query_2',
    HangarQuery3: 'Hangar_Query_3',
    HangarQuery4: 'Hangar_Query_4',
    HangarQuery5: 'Hangar_Query_5',
    HangarQuery6: 'Hangar_Query_6',
    HangarQuery7: 'Hangar_Query_7',
    HangarTake1: 'Hangar_Take_1',
    HangarTake2: 'Hangar_Take_2',
    HangarTake3: 'Hangar_Take_3',
    HangarTake4: 'Hangar_Take_4',
    HangarTake5: 'Hangar_Take_5',
    HangarTake6: 'Hangar_Take_6',
    HangarTake7: 'Hangar_Take_7',
    JuniorAccountant: 'Junior_Accountant',
    PersonnelManager: 'Personnel_Manager',
    RentFactoryFacility: 'Rent_Factory_Facility',
    RentOffice: 'Rent_Office',
    RentResearchFacility: 'Rent_Research_Facility',
    SecurityOfficer: 'Security_Officer',
    SkillPlanManager: 'Skill_Plan_Manager',
    StarbaseDefenseOperator: 'Starbase_Defense_Operator',
    StarbaseFuelTechnician: 'Starbase_Fuel_Technician',
    StationManager: 'Station_Manager',
    Trader: 'Trader'
} as const;
export type GetCorporationsCorporationIdTitles200OkGrantableRolesAtHqEnum = typeof GetCorporationsCorporationIdTitles200OkGrantableRolesAtHqEnum[keyof typeof GetCorporationsCorporationIdTitles200OkGrantableRolesAtHqEnum];

/**
 * @export
 */
export const GetCorporationsCorporationIdTitles200OkGrantableRolesAtOtherEnum = {
    AccountTake1: 'Account_Take_1',
    AccountTake2: 'Account_Take_2',
    AccountTake3: 'Account_Take_3',
    AccountTake4: 'Account_Take_4',
    AccountTake5: 'Account_Take_5',
    AccountTake6: 'Account_Take_6',
    AccountTake7: 'Account_Take_7',
    Accountant: 'Accountant',
    Auditor: 'Auditor',
    CommunicationsOfficer: 'Communications_Officer',
    ConfigEquipment: 'Config_Equipment',
    ConfigStarbaseEquipment: 'Config_Starbase_Equipment',
    ContainerTake1: 'Container_Take_1',
    ContainerTake2: 'Container_Take_2',
    ContainerTake3: 'Container_Take_3',
    ContainerTake4: 'Container_Take_4',
    ContainerTake5: 'Container_Take_5',
    ContainerTake6: 'Container_Take_6',
    ContainerTake7: 'Container_Take_7',
    ContractManager: 'Contract_Manager',
    Diplomat: 'Diplomat',
    Director: 'Director',
    FactoryManager: 'Factory_Manager',
    FittingManager: 'Fitting_Manager',
    HangarQuery1: 'Hangar_Query_1',
    HangarQuery2: 'Hangar_Query_2',
    HangarQuery3: 'Hangar_Query_3',
    HangarQuery4: 'Hangar_Query_4',
    HangarQuery5: 'Hangar_Query_5',
    HangarQuery6: 'Hangar_Query_6',
    HangarQuery7: 'Hangar_Query_7',
    HangarTake1: 'Hangar_Take_1',
    HangarTake2: 'Hangar_Take_2',
    HangarTake3: 'Hangar_Take_3',
    HangarTake4: 'Hangar_Take_4',
    HangarTake5: 'Hangar_Take_5',
    HangarTake6: 'Hangar_Take_6',
    HangarTake7: 'Hangar_Take_7',
    JuniorAccountant: 'Junior_Accountant',
    PersonnelManager: 'Personnel_Manager',
    RentFactoryFacility: 'Rent_Factory_Facility',
    RentOffice: 'Rent_Office',
    RentResearchFacility: 'Rent_Research_Facility',
    SecurityOfficer: 'Security_Officer',
    SkillPlanManager: 'Skill_Plan_Manager',
    StarbaseDefenseOperator: 'Starbase_Defense_Operator',
    StarbaseFuelTechnician: 'Starbase_Fuel_Technician',
    StationManager: 'Station_Manager',
    Trader: 'Trader'
} as const;
export type GetCorporationsCorporationIdTitles200OkGrantableRolesAtOtherEnum = typeof GetCorporationsCorporationIdTitles200OkGrantableRolesAtOtherEnum[keyof typeof GetCorporationsCorporationIdTitles200OkGrantableRolesAtOtherEnum];

/**
 * @export
 */
export const GetCorporationsCorporationIdTitles200OkRolesEnum = {
    AccountTake1: 'Account_Take_1',
    AccountTake2: 'Account_Take_2',
    AccountTake3: 'Account_Take_3',
    AccountTake4: 'Account_Take_4',
    AccountTake5: 'Account_Take_5',
    AccountTake6: 'Account_Take_6',
    AccountTake7: 'Account_Take_7',
    Accountant: 'Accountant',
    Auditor: 'Auditor',
    CommunicationsOfficer: 'Communications_Officer',
    ConfigEquipment: 'Config_Equipment',
    ConfigStarbaseEquipment: 'Config_Starbase_Equipment',
    ContainerTake1: 'Container_Take_1',
    ContainerTake2: 'Container_Take_2',
    ContainerTake3: 'Container_Take_3',
    ContainerTake4: 'Container_Take_4',
    ContainerTake5: 'Container_Take_5',
    ContainerTake6: 'Container_Take_6',
    ContainerTake7: 'Container_Take_7',
    ContractManager: 'Contract_Manager',
    Diplomat: 'Diplomat',
    Director: 'Director',
    FactoryManager: 'Factory_Manager',
    FittingManager: 'Fitting_Manager',
    HangarQuery1: 'Hangar_Query_1',
    HangarQuery2: 'Hangar_Query_2',
    HangarQuery3: 'Hangar_Query_3',
    HangarQuery4: 'Hangar_Query_4',
    HangarQuery5: 'Hangar_Query_5',
    HangarQuery6: 'Hangar_Query_6',
    HangarQuery7: 'Hangar_Query_7',
    HangarTake1: 'Hangar_Take_1',
    HangarTake2: 'Hangar_Take_2',
    HangarTake3: 'Hangar_Take_3',
    HangarTake4: 'Hangar_Take_4',
    HangarTake5: 'Hangar_Take_5',
    HangarTake6: 'Hangar_Take_6',
    HangarTake7: 'Hangar_Take_7',
    JuniorAccountant: 'Junior_Accountant',
    PersonnelManager: 'Personnel_Manager',
    RentFactoryFacility: 'Rent_Factory_Facility',
    RentOffice: 'Rent_Office',
    RentResearchFacility: 'Rent_Research_Facility',
    SecurityOfficer: 'Security_Officer',
    SkillPlanManager: 'Skill_Plan_Manager',
    StarbaseDefenseOperator: 'Starbase_Defense_Operator',
    StarbaseFuelTechnician: 'Starbase_Fuel_Technician',
    StationManager: 'Station_Manager',
    Trader: 'Trader'
} as const;
export type GetCorporationsCorporationIdTitles200OkRolesEnum = typeof GetCorporationsCorporationIdTitles200OkRolesEnum[keyof typeof GetCorporationsCorporationIdTitles200OkRolesEnum];

/**
 * @export
 */
export const GetCorporationsCorporationIdTitles200OkRolesAtBaseEnum = {
    AccountTake1: 'Account_Take_1',
    AccountTake2: 'Account_Take_2',
    AccountTake3: 'Account_Take_3',
    AccountTake4: 'Account_Take_4',
    AccountTake5: 'Account_Take_5',
    AccountTake6: 'Account_Take_6',
    AccountTake7: 'Account_Take_7',
    Accountant: 'Accountant',
    Auditor: 'Auditor',
    CommunicationsOfficer: 'Communications_Officer',
    ConfigEquipment: 'Config_Equipment',
    ConfigStarbaseEquipment: 'Config_Starbase_Equipment',
    ContainerTake1: 'Container_Take_1',
    ContainerTake2: 'Container_Take_2',
    ContainerTake3: 'Container_Take_3',
    ContainerTake4: 'Container_Take_4',
    ContainerTake5: 'Container_Take_5',
    ContainerTake6: 'Container_Take_6',
    ContainerTake7: 'Container_Take_7',
    ContractManager: 'Contract_Manager',
    Diplomat: 'Diplomat',
    Director: 'Director',
    FactoryManager: 'Factory_Manager',
    FittingManager: 'Fitting_Manager',
    HangarQuery1: 'Hangar_Query_1',
    HangarQuery2: 'Hangar_Query_2',
    HangarQuery3: 'Hangar_Query_3',
    HangarQuery4: 'Hangar_Query_4',
    HangarQuery5: 'Hangar_Query_5',
    HangarQuery6: 'Hangar_Query_6',
    HangarQuery7: 'Hangar_Query_7',
    HangarTake1: 'Hangar_Take_1',
    HangarTake2: 'Hangar_Take_2',
    HangarTake3: 'Hangar_Take_3',
    HangarTake4: 'Hangar_Take_4',
    HangarTake5: 'Hangar_Take_5',
    HangarTake6: 'Hangar_Take_6',
    HangarTake7: 'Hangar_Take_7',
    JuniorAccountant: 'Junior_Accountant',
    PersonnelManager: 'Personnel_Manager',
    RentFactoryFacility: 'Rent_Factory_Facility',
    RentOffice: 'Rent_Office',
    RentResearchFacility: 'Rent_Research_Facility',
    SecurityOfficer: 'Security_Officer',
    SkillPlanManager: 'Skill_Plan_Manager',
    StarbaseDefenseOperator: 'Starbase_Defense_Operator',
    StarbaseFuelTechnician: 'Starbase_Fuel_Technician',
    StationManager: 'Station_Manager',
    Trader: 'Trader'
} as const;
export type GetCorporationsCorporationIdTitles200OkRolesAtBaseEnum = typeof GetCorporationsCorporationIdTitles200OkRolesAtBaseEnum[keyof typeof GetCorporationsCorporationIdTitles200OkRolesAtBaseEnum];

/**
 * @export
 */
export const GetCorporationsCorporationIdTitles200OkRolesAtHqEnum = {
    AccountTake1: 'Account_Take_1',
    AccountTake2: 'Account_Take_2',
    AccountTake3: 'Account_Take_3',
    AccountTake4: 'Account_Take_4',
    AccountTake5: 'Account_Take_5',
    AccountTake6: 'Account_Take_6',
    AccountTake7: 'Account_Take_7',
    Accountant: 'Accountant',
    Auditor: 'Auditor',
    CommunicationsOfficer: 'Communications_Officer',
    ConfigEquipment: 'Config_Equipment',
    ConfigStarbaseEquipment: 'Config_Starbase_Equipment',
    ContainerTake1: 'Container_Take_1',
    ContainerTake2: 'Container_Take_2',
    ContainerTake3: 'Container_Take_3',
    ContainerTake4: 'Container_Take_4',
    ContainerTake5: 'Container_Take_5',
    ContainerTake6: 'Container_Take_6',
    ContainerTake7: 'Container_Take_7',
    ContractManager: 'Contract_Manager',
    Diplomat: 'Diplomat',
    Director: 'Director',
    FactoryManager: 'Factory_Manager',
    FittingManager: 'Fitting_Manager',
    HangarQuery1: 'Hangar_Query_1',
    HangarQuery2: 'Hangar_Query_2',
    HangarQuery3: 'Hangar_Query_3',
    HangarQuery4: 'Hangar_Query_4',
    HangarQuery5: 'Hangar_Query_5',
    HangarQuery6: 'Hangar_Query_6',
    HangarQuery7: 'Hangar_Query_7',
    HangarTake1: 'Hangar_Take_1',
    HangarTake2: 'Hangar_Take_2',
    HangarTake3: 'Hangar_Take_3',
    HangarTake4: 'Hangar_Take_4',
    HangarTake5: 'Hangar_Take_5',
    HangarTake6: 'Hangar_Take_6',
    HangarTake7: 'Hangar_Take_7',
    JuniorAccountant: 'Junior_Accountant',
    PersonnelManager: 'Personnel_Manager',
    RentFactoryFacility: 'Rent_Factory_Facility',
    RentOffice: 'Rent_Office',
    RentResearchFacility: 'Rent_Research_Facility',
    SecurityOfficer: 'Security_Officer',
    SkillPlanManager: 'Skill_Plan_Manager',
    StarbaseDefenseOperator: 'Starbase_Defense_Operator',
    StarbaseFuelTechnician: 'Starbase_Fuel_Technician',
    StationManager: 'Station_Manager',
    Trader: 'Trader'
} as const;
export type GetCorporationsCorporationIdTitles200OkRolesAtHqEnum = typeof GetCorporationsCorporationIdTitles200OkRolesAtHqEnum[keyof typeof GetCorporationsCorporationIdTitles200OkRolesAtHqEnum];

/**
 * @export
 */
export const GetCorporationsCorporationIdTitles200OkRolesAtOtherEnum = {
    AccountTake1: 'Account_Take_1',
    AccountTake2: 'Account_Take_2',
    AccountTake3: 'Account_Take_3',
    AccountTake4: 'Account_Take_4',
    AccountTake5: 'Account_Take_5',
    AccountTake6: 'Account_Take_6',
    AccountTake7: 'Account_Take_7',
    Accountant: 'Accountant',
    Auditor: 'Auditor',
    CommunicationsOfficer: 'Communications_Officer',
    ConfigEquipment: 'Config_Equipment',
    ConfigStarbaseEquipment: 'Config_Starbase_Equipment',
    ContainerTake1: 'Container_Take_1',
    ContainerTake2: 'Container_Take_2',
    ContainerTake3: 'Container_Take_3',
    ContainerTake4: 'Container_Take_4',
    ContainerTake5: 'Container_Take_5',
    ContainerTake6: 'Container_Take_6',
    ContainerTake7: 'Container_Take_7',
    ContractManager: 'Contract_Manager',
    Diplomat: 'Diplomat',
    Director: 'Director',
    FactoryManager: 'Factory_Manager',
    FittingManager: 'Fitting_Manager',
    HangarQuery1: 'Hangar_Query_1',
    HangarQuery2: 'Hangar_Query_2',
    HangarQuery3: 'Hangar_Query_3',
    HangarQuery4: 'Hangar_Query_4',
    HangarQuery5: 'Hangar_Query_5',
    HangarQuery6: 'Hangar_Query_6',
    HangarQuery7: 'Hangar_Query_7',
    HangarTake1: 'Hangar_Take_1',
    HangarTake2: 'Hangar_Take_2',
    HangarTake3: 'Hangar_Take_3',
    HangarTake4: 'Hangar_Take_4',
    HangarTake5: 'Hangar_Take_5',
    HangarTake6: 'Hangar_Take_6',
    HangarTake7: 'Hangar_Take_7',
    JuniorAccountant: 'Junior_Accountant',
    PersonnelManager: 'Personnel_Manager',
    RentFactoryFacility: 'Rent_Factory_Facility',
    RentOffice: 'Rent_Office',
    RentResearchFacility: 'Rent_Research_Facility',
    SecurityOfficer: 'Security_Officer',
    SkillPlanManager: 'Skill_Plan_Manager',
    StarbaseDefenseOperator: 'Starbase_Defense_Operator',
    StarbaseFuelTechnician: 'Starbase_Fuel_Technician',
    StationManager: 'Station_Manager',
    Trader: 'Trader'
} as const;
export type GetCorporationsCorporationIdTitles200OkRolesAtOtherEnum = typeof GetCorporationsCorporationIdTitles200OkRolesAtOtherEnum[keyof typeof GetCorporationsCorporationIdTitles200OkRolesAtOtherEnum];


/**
 * Check if a given object implements the GetCorporationsCorporationIdTitles200Ok interface.
 */
export function instanceOfGetCorporationsCorporationIdTitles200Ok(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function GetCorporationsCorporationIdTitles200OkFromJSON(json: any): GetCorporationsCorporationIdTitles200Ok {
    return GetCorporationsCorporationIdTitles200OkFromJSONTyped(json, false);
}

export function GetCorporationsCorporationIdTitles200OkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCorporationsCorporationIdTitles200Ok {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'grantableRoles': !exists(json, 'grantable_roles') ? undefined : json['grantable_roles'],
        'grantableRolesAtBase': !exists(json, 'grantable_roles_at_base') ? undefined : json['grantable_roles_at_base'],
        'grantableRolesAtHq': !exists(json, 'grantable_roles_at_hq') ? undefined : json['grantable_roles_at_hq'],
        'grantableRolesAtOther': !exists(json, 'grantable_roles_at_other') ? undefined : json['grantable_roles_at_other'],
        'name': !exists(json, 'name') ? undefined : json['name'],
        'roles': !exists(json, 'roles') ? undefined : json['roles'],
        'rolesAtBase': !exists(json, 'roles_at_base') ? undefined : json['roles_at_base'],
        'rolesAtHq': !exists(json, 'roles_at_hq') ? undefined : json['roles_at_hq'],
        'rolesAtOther': !exists(json, 'roles_at_other') ? undefined : json['roles_at_other'],
        'titleId': !exists(json, 'title_id') ? undefined : json['title_id'],
    };
}

export function GetCorporationsCorporationIdTitles200OkToJSON(value?: GetCorporationsCorporationIdTitles200Ok | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'grantable_roles': value.grantableRoles,
        'grantable_roles_at_base': value.grantableRolesAtBase,
        'grantable_roles_at_hq': value.grantableRolesAtHq,
        'grantable_roles_at_other': value.grantableRolesAtOther,
        'name': value.name,
        'roles': value.roles,
        'roles_at_base': value.rolesAtBase,
        'roles_at_hq': value.rolesAtHq,
        'roles_at_other': value.rolesAtOther,
        'title_id': value.titleId,
    };
}

