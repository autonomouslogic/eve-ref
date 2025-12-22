
import {marketApi} from "~/esi";
import {DOMAIN, HEIMATAR, METROPOLIS, GPMR_01, SINQ_LAISON, THE_FORGE} from "~/lib/regionConstants";
import {PLEX_TYPE_ID} from "~/lib/typeConstants";
import {
    GetMarketsRegionIdOrdersOrderTypeEnum,
    type GetMarketsRegionIdOrdersRequest,
    type MarketsRegionIdOrdersGetInner
} from "~/esi-openapi";

export interface HubStation {
    systemName: string,
    regionId: number,
    stationId: number | undefined
}

export const PLEX_REGION_HUB_STATION = {
    systemName: "GPMR-01",
    regionId: GPMR_01
} as HubStation;

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

export async function getOrders(orderType: GetMarketsRegionIdOrdersOrderTypeEnum, typeId: number, regionId: number): Promise<Array<MarketsRegionIdOrdersGetInner>> {
    return marketApi.getMarketsRegionIdOrders({
        typeId: typeId,
        regionId: regionId,
        orderType
    } as GetMarketsRegionIdOrdersRequest);
}

/**
 * Returns the Jita sell price for the supplied type.
 * If the request type is PLEX, the PLEX market region is automatically used.
 * @param typeId
 */
export async function getJitaSellPrice(typeId: number) {
    var regionId: number | undefined = undefined;
    var hub: HubStation | undefined = undefined;
    if (typeId == PLEX_TYPE_ID) {
        regionId = GPMR_01;
    }
    else {
        hub = HUB_STATIONS.get("Jita");
        regionId = hub?.regionId;
    }
    if (regionId == undefined) {
        return undefined;
    }
    var orders  = (await getOrders(GetMarketsRegionIdOrdersOrderTypeEnum.Sell, typeId, regionId));
    if (hub != undefined) {
        orders = orders.filter(e => e.locationId == hub?.stationId)
    }
        const sellPrice = orders
        .map(e => e.price)
        .sort((a, b) => a - b)
        .shift();
    return sellPrice;
}
