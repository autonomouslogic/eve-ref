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
 * @interface GetCorporationsCorporationIdAssets200Ok
 */
export interface GetCorporationsCorporationIdAssets200Ok {
    /**
     * is_blueprint_copy boolean
     * @type {boolean}
     * @memberof GetCorporationsCorporationIdAssets200Ok
     */
    isBlueprintCopy?: boolean;
    /**
     * is_singleton boolean
     * @type {boolean}
     * @memberof GetCorporationsCorporationIdAssets200Ok
     */
    isSingleton: boolean;
    /**
     * item_id integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdAssets200Ok
     */
    itemId: number;
    /**
     * location_flag string
     * @type {string}
     * @memberof GetCorporationsCorporationIdAssets200Ok
     */
    locationFlag: GetCorporationsCorporationIdAssets200OkLocationFlagEnum;
    /**
     * location_id integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdAssets200Ok
     */
    locationId: number;
    /**
     * location_type string
     * @type {string}
     * @memberof GetCorporationsCorporationIdAssets200Ok
     */
    locationType: GetCorporationsCorporationIdAssets200OkLocationTypeEnum;
    /**
     * quantity integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdAssets200Ok
     */
    quantity: number;
    /**
     * type_id integer
     * @type {number}
     * @memberof GetCorporationsCorporationIdAssets200Ok
     */
    typeId: number;
}


/**
 * @export
 */
export const GetCorporationsCorporationIdAssets200OkLocationFlagEnum = {
    AssetSafety: 'AssetSafety',
    AutoFit: 'AutoFit',
    Bonus: 'Bonus',
    Booster: 'Booster',
    BoosterBay: 'BoosterBay',
    Capsule: 'Capsule',
    Cargo: 'Cargo',
    CorpDeliveries: 'CorpDeliveries',
    CorpSag1: 'CorpSAG1',
    CorpSag2: 'CorpSAG2',
    CorpSag3: 'CorpSAG3',
    CorpSag4: 'CorpSAG4',
    CorpSag5: 'CorpSAG5',
    CorpSag6: 'CorpSAG6',
    CorpSag7: 'CorpSAG7',
    CorporationGoalDeliveries: 'CorporationGoalDeliveries',
    CrateLoot: 'CrateLoot',
    Deliveries: 'Deliveries',
    DroneBay: 'DroneBay',
    DustBattle: 'DustBattle',
    DustDatabank: 'DustDatabank',
    FighterBay: 'FighterBay',
    FighterTube0: 'FighterTube0',
    FighterTube1: 'FighterTube1',
    FighterTube2: 'FighterTube2',
    FighterTube3: 'FighterTube3',
    FighterTube4: 'FighterTube4',
    FleetHangar: 'FleetHangar',
    FrigateEscapeBay: 'FrigateEscapeBay',
    Hangar: 'Hangar',
    HangarAll: 'HangarAll',
    HiSlot0: 'HiSlot0',
    HiSlot1: 'HiSlot1',
    HiSlot2: 'HiSlot2',
    HiSlot3: 'HiSlot3',
    HiSlot4: 'HiSlot4',
    HiSlot5: 'HiSlot5',
    HiSlot6: 'HiSlot6',
    HiSlot7: 'HiSlot7',
    HiddenModifiers: 'HiddenModifiers',
    Implant: 'Implant',
    Impounded: 'Impounded',
    InfrastructureHangar: 'InfrastructureHangar',
    JunkyardReprocessed: 'JunkyardReprocessed',
    JunkyardTrashed: 'JunkyardTrashed',
    LoSlot0: 'LoSlot0',
    LoSlot1: 'LoSlot1',
    LoSlot2: 'LoSlot2',
    LoSlot3: 'LoSlot3',
    LoSlot4: 'LoSlot4',
    LoSlot5: 'LoSlot5',
    LoSlot6: 'LoSlot6',
    LoSlot7: 'LoSlot7',
    Locked: 'Locked',
    MedSlot0: 'MedSlot0',
    MedSlot1: 'MedSlot1',
    MedSlot2: 'MedSlot2',
    MedSlot3: 'MedSlot3',
    MedSlot4: 'MedSlot4',
    MedSlot5: 'MedSlot5',
    MedSlot6: 'MedSlot6',
    MedSlot7: 'MedSlot7',
    MobileDepotHold: 'MobileDepotHold',
    MoonMaterialBay: 'MoonMaterialBay',
    OfficeFolder: 'OfficeFolder',
    Pilot: 'Pilot',
    PlanetSurface: 'PlanetSurface',
    QuafeBay: 'QuafeBay',
    QuantumCoreRoom: 'QuantumCoreRoom',
    Reward: 'Reward',
    RigSlot0: 'RigSlot0',
    RigSlot1: 'RigSlot1',
    RigSlot2: 'RigSlot2',
    RigSlot3: 'RigSlot3',
    RigSlot4: 'RigSlot4',
    RigSlot5: 'RigSlot5',
    RigSlot6: 'RigSlot6',
    RigSlot7: 'RigSlot7',
    SecondaryStorage: 'SecondaryStorage',
    ServiceSlot0: 'ServiceSlot0',
    ServiceSlot1: 'ServiceSlot1',
    ServiceSlot2: 'ServiceSlot2',
    ServiceSlot3: 'ServiceSlot3',
    ServiceSlot4: 'ServiceSlot4',
    ServiceSlot5: 'ServiceSlot5',
    ServiceSlot6: 'ServiceSlot6',
    ServiceSlot7: 'ServiceSlot7',
    ShipHangar: 'ShipHangar',
    ShipOffline: 'ShipOffline',
    Skill: 'Skill',
    SkillInTraining: 'SkillInTraining',
    SpecializedAmmoHold: 'SpecializedAmmoHold',
    SpecializedAsteroidHold: 'SpecializedAsteroidHold',
    SpecializedCommandCenterHold: 'SpecializedCommandCenterHold',
    SpecializedFuelBay: 'SpecializedFuelBay',
    SpecializedGasHold: 'SpecializedGasHold',
    SpecializedIceHold: 'SpecializedIceHold',
    SpecializedIndustrialShipHold: 'SpecializedIndustrialShipHold',
    SpecializedLargeShipHold: 'SpecializedLargeShipHold',
    SpecializedMaterialBay: 'SpecializedMaterialBay',
    SpecializedMediumShipHold: 'SpecializedMediumShipHold',
    SpecializedMineralHold: 'SpecializedMineralHold',
    SpecializedOreHold: 'SpecializedOreHold',
    SpecializedPlanetaryCommoditiesHold: 'SpecializedPlanetaryCommoditiesHold',
    SpecializedSalvageHold: 'SpecializedSalvageHold',
    SpecializedShipHold: 'SpecializedShipHold',
    SpecializedSmallShipHold: 'SpecializedSmallShipHold',
    StructureActive: 'StructureActive',
    StructureFuel: 'StructureFuel',
    StructureInactive: 'StructureInactive',
    StructureOffline: 'StructureOffline',
    SubSystemBay: 'SubSystemBay',
    SubSystemSlot0: 'SubSystemSlot0',
    SubSystemSlot1: 'SubSystemSlot1',
    SubSystemSlot2: 'SubSystemSlot2',
    SubSystemSlot3: 'SubSystemSlot3',
    SubSystemSlot4: 'SubSystemSlot4',
    SubSystemSlot5: 'SubSystemSlot5',
    SubSystemSlot6: 'SubSystemSlot6',
    SubSystemSlot7: 'SubSystemSlot7',
    Unlocked: 'Unlocked',
    Wallet: 'Wallet',
    Wardrobe: 'Wardrobe'
} as const;
export type GetCorporationsCorporationIdAssets200OkLocationFlagEnum = typeof GetCorporationsCorporationIdAssets200OkLocationFlagEnum[keyof typeof GetCorporationsCorporationIdAssets200OkLocationFlagEnum];

/**
 * @export
 */
export const GetCorporationsCorporationIdAssets200OkLocationTypeEnum = {
    Station: 'station',
    SolarSystem: 'solar_system',
    Item: 'item',
    Other: 'other'
} as const;
export type GetCorporationsCorporationIdAssets200OkLocationTypeEnum = typeof GetCorporationsCorporationIdAssets200OkLocationTypeEnum[keyof typeof GetCorporationsCorporationIdAssets200OkLocationTypeEnum];


/**
 * Check if a given object implements the GetCorporationsCorporationIdAssets200Ok interface.
 */
export function instanceOfGetCorporationsCorporationIdAssets200Ok(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "isSingleton" in value;
    isInstance = isInstance && "itemId" in value;
    isInstance = isInstance && "locationFlag" in value;
    isInstance = isInstance && "locationId" in value;
    isInstance = isInstance && "locationType" in value;
    isInstance = isInstance && "quantity" in value;
    isInstance = isInstance && "typeId" in value;

    return isInstance;
}

export function GetCorporationsCorporationIdAssets200OkFromJSON(json: any): GetCorporationsCorporationIdAssets200Ok {
    return GetCorporationsCorporationIdAssets200OkFromJSONTyped(json, false);
}

export function GetCorporationsCorporationIdAssets200OkFromJSONTyped(json: any, ignoreDiscriminator: boolean): GetCorporationsCorporationIdAssets200Ok {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'isBlueprintCopy': !exists(json, 'is_blueprint_copy') ? undefined : json['is_blueprint_copy'],
        'isSingleton': json['is_singleton'],
        'itemId': json['item_id'],
        'locationFlag': json['location_flag'],
        'locationId': json['location_id'],
        'locationType': json['location_type'],
        'quantity': json['quantity'],
        'typeId': json['type_id'],
    };
}

export function GetCorporationsCorporationIdAssets200OkToJSON(value?: GetCorporationsCorporationIdAssets200Ok | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'is_blueprint_copy': value.isBlueprintCopy,
        'is_singleton': value.isSingleton,
        'item_id': value.itemId,
        'location_flag': value.locationFlag,
        'location_id': value.locationId,
        'location_type': value.locationType,
        'quantity': value.quantity,
        'type_id': value.typeId,
    };
}

