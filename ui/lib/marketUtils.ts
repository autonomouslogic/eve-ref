import {GetMarketsRegionIdOrdersDatasourceEnum, GetMarketsRegionIdOrdersOrderTypeEnum} from "~/esi-openapi";
import {marketApi} from "~/esi";

export const THE_FORGE_REGION_ID = 10000002;

export const HUB_STATION_IDS = new Map<string, number>();
HUB_STATION_IDS.set("Jita", 60003760); // Jita IV - Moon 4 - Caldari Navy Assembly Plant
HUB_STATION_IDS.set("Amarr", 60008494); // Amarr VIII (Oris) - Emperor Family Academy
HUB_STATION_IDS.set("Rens", 60004588); // Rens VI - Moon 8 - Brutor Tribe Treasury
HUB_STATION_IDS.set("Dodixie", 60011866); // Dodixie IX - Moon 20 - Federation Navy Assembly Plant
HUB_STATION_IDS.set("Hek", 60005686); // Hek VIII - Moon 12 - Boundless Creation Factory
HUB_STATION_IDS.set("Misaba", 60006658); // Misaba V - Moon 3 - Zoar and Sons Factory

export async function getOrders(orderType: GetMarketsRegionIdOrdersOrderTypeEnum, typeId: number, regionId: number) {
    return marketApi.getMarketsRegionIdOrders({
        typeId: typeId,
        regionId: regionId,
        orderType,
        datasource: GetMarketsRegionIdOrdersDatasourceEnum.Tranquility
    });
}

export async function getJitaSellPrice(typeId: number) {
    const jitaStation = HUB_STATION_IDS.get("Jita");
    const sellPrice = (await getOrders(GetMarketsRegionIdOrdersOrderTypeEnum.Sell, typeId, THE_FORGE_REGION_ID))
        .filter(e => e.locationId == jitaStation)
        .map(e => e.price)
        .sort((a, b) => a - b)
        .shift();
    return sellPrice;
}
