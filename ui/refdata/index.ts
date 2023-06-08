import {InventoryType} from "~/refdata-openapi";

const BASE_URL = "https://ref-data.everef.net";

export async function getInventoryType(typeId: number): Promise<InventoryType> {
    const url = `${BASE_URL}/types/${typeId}`;
    return await useFetch(url).data as InventoryType;
}
