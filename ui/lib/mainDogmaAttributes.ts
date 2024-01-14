// This file contains the main dogma attributes used for quick comparison in the UI, mainly the VariationsCard.

import type {InventoryType} from "~/refdata-openapi";
import {SHIP} from "~/lib/categoryConstants";
import refdataApi from "~/refdata";

const categoryAttributeNames: { [key: string]: string[] } = {};
categoryAttributeNames[SHIP] = [
    "shieldCapacity", "armorHP", "hp",
    "powerOutput", "cpuOutput", "capacitorCapacity",
    "hiSlots", "medSlots", "lowSlots",
    "turretSlotsLeft", "launcherSlotsLeft"
];

async function getCategoryDogma(type: InventoryType): Promise<string[] | undefined> {
    if (type.groupId === undefined) {
        return undefined;
    }
    const group = await refdataApi.getGroup({groupId: type.groupId});
    if (group === undefined) {
        return undefined;
    }
    const id = `${group.categoryId}`;
    if (categoryAttributeNames[id]) {
        return categoryAttributeNames[id];
    }
    return undefined;
}

export async function getMainDogma(type: InventoryType): Promise<string[]> {
    return await getCategoryDogma(type) || [];
}
