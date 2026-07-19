<script setup lang="ts">
import {GetMarketsRegionIdOrdersOrderTypeEnum,} from "~/esi-openapi";
import UnitValue from "~/components/dogma/UnitValue.vue";
import {getOrders, HUB_STATIONS} from "~/lib/marketUtils";
import {MONEY} from "~/lib/unitConstants";
import {IndustryApi} from "~/api-openapi";

interface Props {
	typeId: number,
	orderType?: "sell" | "buy"
	regionId?: number,
	stationId?: number
}
const props = withDefaults(defineProps<Props>(), {
	orderType: "sell",
	regionId: HUB_STATIONS.get("Jita")?.regionId,
	stationId: HUB_STATIONS.get("Jita")?.stationId,
});

let orderTypeEnum: GetMarketsRegionIdOrdersOrderTypeEnum;
let comparator: (a: number, b: number) => number;
if (props.orderType == "sell") {
	orderTypeEnum = GetMarketsRegionIdOrdersOrderTypeEnum.Sell;
	comparator = (a: number, b: number) => a - b;
}
else if (props.orderType == "buy") {
	orderTypeEnum = GetMarketsRegionIdOrdersOrderTypeEnum.Buy;
	comparator = (a: number, b: number) => -(a - b);
}
else {
	throw new Error(`Invalid order type: ${props.orderType}`);
}

const	allOrders = await getOrders(orderTypeEnum, props.typeId, props.regionId);
const stationOrders = !allOrders ? undefined : allOrders
	.filter(e => e.locationId == props.stationId);
const price = !stationOrders ? undefined : stationOrders
	.map(e => e.price)
	.sort(comparator)
	[0];

var api = new IndustryApi();
const cost = await api.industryCost(
	{ product_id: 645 }
);

</script>

<template>
	<UnitValue v-if="price" :unit-id="MONEY" :value="price" />
	<span v-else>-</span>
</template>
