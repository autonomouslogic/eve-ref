// This file contains the main dogma attributes used for quick comparison in the UI, mainly the VariationsCard.

import type {InventoryType} from "~/refdata-openapi";
import {CHARGES, SHIP} from "~/lib/categoryConstants";
import refdataApi from "~/refdata";

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
    return getCategoryDogma(group.categoryId);
}

export async function getMainDogma(type: InventoryType): Promise<string[]> {
    return await getGroupDogma(type.groupId) || [];
}
