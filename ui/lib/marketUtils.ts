import {GetMarketsRegionIdOrdersDatasourceEnum, GetMarketsRegionIdOrdersOrderTypeEnum} from "~/esi-openapi";
import {marketApi} from "~/esi";
import type {DogmaAttribute, InventoryType} from "~/refdata-openapi";
import {DOMAIN, HEIMATAR, METROPOLIS, SINQ_LAISON, THE_FORGE} from "~/lib/regionConstants";

export interface HubStation {
    systemName: string,
    stationId: number,
    regionId: number
}

export const HUB_STATIONS = new Map<string, HubStation>();
// Jita IV - Moon 4 - Caldari Navy Assembly Plant
HUB_STATIONS.set("Jita", {
    systemName: "Jita",
    stationId: 60003760,
    regionId: THE_FORGE
} as HubStation);
// Amarr VIII (Oris) - Emperor Family Academy
HUB_STATIONS.set("Amarr", {
    systemName: "Amarr",
    stationId: 60008494,
    regionId: DOMAIN
} as HubStation);
// Rens VI - Moon 8 - Brutor Tribe Treasury
HUB_STATIONS.set("Rens", {
    systemName: "Rens",
    stationId: 60004588,
    regionId: HEIMATAR
} as HubStation);
// Dodixie IX - Moon 20 - Federation Navy Assembly Plant
HUB_STATIONS.set("Dodixie", {
    systemName: "Dodixie",
    stationId: 60011866,
    regionId: SINQ_LAISON
} as HubStation);
// Hek VIII - Moon 12 - Boundless Creation Factory
HUB_STATIONS.set("Hek", {
    systemName: "Hek",
    stationId: 60005686,
    regionId: METROPOLIS
} as HubStation);
// Misaba V - Moon 3 - Zoar and Sons Factory
HUB_STATIONS.set("Misaba", {
    systemName: "Misaba",
    stationId: 60006658,
    regionId: DOMAIN
} as HubStation);

export async function getOrders(orderType: GetMarketsRegionIdOrdersOrderTypeEnum, typeId: number, regionId: number) {
    return marketApi.getMarketsRegionIdOrders({
        typeId: typeId,
        regionId: regionId,
        orderType,
        datasource: GetMarketsRegionIdOrdersDatasourceEnum.Tranquility
    });
}

export async function getJitaSellPrice(typeId: number) {
    const jitaHub = HUB_STATIONS.get("Jita");
    if (jitaHub == undefined) {
        return undefined;
    }
    const sellPrice = (await getOrders(GetMarketsRegionIdOrdersOrderTypeEnum.Sell, typeId, jitaHub.regionId))
        .filter(e => e.locationId == jitaHub?.stationId)
        .map(e => e.price)
        .sort((a, b) => a - b)
        .shift();
    return sellPrice;
}
