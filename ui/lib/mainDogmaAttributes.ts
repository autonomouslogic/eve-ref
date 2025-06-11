// This file contains the main dogma attributes used for quick comparison in the UI, mainly the VariationsCard.

import type {InventoryType} from "~/refdata-openapi";
import {
    CHARGES,
    DECRYPTORS,
    DEPLOYABLE,
    DRONE,
    IMPLANT,
    MODULE,
    SHIP,
    STRUCTURE_MODULE,
    SUBSYSTEM
} from "~/lib/categoryConstants";
import refdataApi from "~/refdata";
import {
    AUDIT_LOG_SECURE_CONTAINER, CAPACITOR_BOOST_CHARGE, CARGO_CONTAINER,
    CORE_SUBSYSTEM, DATA_MINERS,
    DEFENSIVE_SUBSYSTEM, FREIGHT_CONTAINER, FREQUENCY_MINING_LASER, GAS_CLOUD_HARVESTERS, MINING_LASER,
    MOBILE_DEPOT, MOBILE_TRACTOR_UNIT, MOBILE_WARP_DISRUPTOR,
    OFFENSIVE_SUBSYSTEM,
    PROPULSION_SUBSYSTEM, SALVAGER, SECURE_CARGO_CONTAINER, STRIP_MINER
} from "~/lib/groupConstants";

const categoryAttributeNames: { [key: string]: string[] } = {};
categoryAttributeNames[SHIP] = [
    "shieldCapacity", "armorHP", "hp",
    "powerOutput", "cpuOutput", "capacitorCapacity",
    "hiSlots", "medSlots", "lowSlots",
    "turretSlotsLeft", "launcherSlotsLeft"
];
categoryAttributeNames[CHARGES] = [
    "chargeSize",
    "emDamage", "explosiveDamage", "kineticDamage", "thermalDamage",
    "maxVelocity", "maxFOFTargetRange",
    "specializationAsteroidYieldMultiplier",
    "trackingSpeedMultiplier",
    "weaponRangeMultiplier",
    "fallofMultiplier",
    "crystalVolatilityChance",
    "capNeedBonus",
    "explosionDelay",
    "aoeCloudSize"
];
categoryAttributeNames[SUBSYSTEM] = [
    "fitsToShipType",
    "hiSlotModifier", "medSlotModifier", "lowSlotModifier"
];
categoryAttributeNames[DECRYPTORS] = [
    "inventionPropabilityMultiplier", "inventionMaxRunModifier", "inventionMEModifier", "inventionTEModifier"
];
categoryAttributeNames[DEPLOYABLE] = [
    "capacity",
    "scanLadarStrength", "scanMagnetometricStrength", "scanRadarStrength", "scanGravimetricStrength",
    "signatureRadius",
    "shieldCapacity", "armorHP", "hp"
];
categoryAttributeNames[DRONE] = [
    "droneBandwidth", "droneBandwidthUsed",
    "emDamage", "explosiveDamage", "kineticDamage", "thermalDamage",
    "maxVelocity",
    "shieldCapacity", "armorHP", "hp",
    "speed", "trackingSpeed"
];
categoryAttributeNames[IMPLANT] = [
    "implantness"
];
categoryAttributeNames[MODULE] = [
    "cpu", "power", "duration", "maxRange",
    "miningAmount",
    "miningWastedVolumeMultiplier",
    "miningWasteProbability"
];
categoryAttributeNames[STRUCTURE_MODULE] = [
    "RefRigMatBonus", "RefRigTimeBonus"
];

const groupAttributeNames: { [key: string]: string[] } = {};
groupAttributeNames[CORE_SUBSYSTEM] = [
    "powerEngineeringOutputBonus", "maxLockedTargetsBonus", "capacitorCapacity"
];
groupAttributeNames[DEFENSIVE_SUBSYSTEM] = [
    "armorHPBonusAdd", "signatureRadius"
];
groupAttributeNames[OFFENSIVE_SUBSYSTEM] = [
    "turretHardPointModifier", "launcherHardPointModifier",
    "maxLockedTargetsBonus",
    "powerOutput", "cpuOutput",
    "droneCapacity", "droneBandwidth",
];
groupAttributeNames[PROPULSION_SUBSYSTEM] = [
    "agilityBonusAdd"
];
groupAttributeNames[CARGO_CONTAINER] = [
    "capacity"
];
groupAttributeNames[FREIGHT_CONTAINER] = [
    "capacity"
];
groupAttributeNames[AUDIT_LOG_SECURE_CONTAINER] = [
    "capacity"
];
groupAttributeNames[SECURE_CARGO_CONTAINER] = [
    "capacity"
];
groupAttributeNames[MOBILE_WARP_DISRUPTOR] = [
    "warpScrambleRange"
];
groupAttributeNames[MOBILE_TRACTOR_UNIT] = [
    "maxLockedTargets", "maxTargetRange"
];
groupAttributeNames[CAPACITOR_BOOST_CHARGE] = [
    "capacitorBonus"
];
groupAttributeNames[SALVAGER] = ["accessDifficultyBonus"];
groupAttributeNames[DATA_MINERS] = [
    "accessDifficultyBonus", "virusCoherence", "virusStrength", "virusElementSlots"
];

export async function getCategoryDogma(categoryId: number | undefined): Promise<string[] | undefined> {
    const id = `${categoryId}`;
    if (categoryAttributeNames[id]) {
        return categoryAttributeNames[id];
    }
    return undefined;
}

export async function getGroupDogma(groupId: number | undefined): Promise<string[] | undefined> {
    if (groupId === undefined) {
        return undefined;
    }
    const group = await refdataApi.getGroup({groupId});
    if (group === undefined) {
        return undefined;
    }
    const categoryDogma = await getCategoryDogma(group.categoryId);
    const id = `${groupId}`;
    if (groupAttributeNames[id]) {
        return (categoryDogma || []).concat(groupAttributeNames[id]);
    }
    return categoryDogma;
}

export async function getMainDogma(type: InventoryType): Promise<string[]> {
    return await getGroupDogma(type.groupId) || [];
}
