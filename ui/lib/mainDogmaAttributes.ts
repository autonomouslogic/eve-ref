// This file contains the main dogma attributes used for quick comparison in the UI, mainly the VariationsCard.

import type {InventoryType} from "~/refdata-openapi";
import {CHARGES, SHIP, SUBSYSTEM} from "~/lib/categoryConstants";
import refdataApi from "~/refdata";
import {CORE_SUBSYSTEM, DEFENSIVE_SUBSYSTEM, OFFENSIVE_SUBSYSTEM, PROPULSION_SUBSYSTEM} from "~/lib/groupConstants";

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
    "specializationAsteroidYieldMultiplier"
];
categoryAttributeNames[SUBSYSTEM] = [
    "fitsToShipType",
    "hiSlotModifier", "medSlotModifier", "lowSlotModifier"
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
