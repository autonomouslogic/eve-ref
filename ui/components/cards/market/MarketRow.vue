<script setup lang="ts">
import {marketApi, universeApi} from "~/esi";
import {GetMarketsRegionIdOrdersDatasourceEnum, GetMarketsRegionIdOrdersOrderTypeEnum} from "~/esi-openapi";
import UnitValue from "~/components/dogma/UnitValue.vue";

const props = defineProps<{
	typeId: number,
	stationId: number
}>();

const station = await universeApi.getUniverseStationsStationId({ stationId: props.stationId });
const system = await universeApi.getUniverseSystemsSystemId({systemId: station.systemId});
const constellation = await universeApi.getUniverseConstellationsConstellationId({constellationId: system.constellationId});

async function getOrders(orderType: GetMarketsRegionIdOrdersOrderTypeEnum) {
	return marketApi.getMarketsRegionIdOrders({
		typeId: props.typeId,
		regionId: constellation.regionId,
		orderType,
		datasource: GetMarketsRegionIdOrdersDatasourceEnum.Tranquility
	});
}

var sellOrders = await getOrders(GetMarketsRegionIdOrdersOrderTypeEnum.Sell);
var buyOrders = await getOrders(GetMarketsRegionIdOrdersOrderTypeEnum.Buy);

const sellPrice = sellOrders.filter(e => e.locationId == station.stationId)
	.map(e => e.price)
	.sort((a, b) => a - b)
	[0];

const buyPrice = buyOrders.filter(e => e.locationId == station.stationId)
	.map(e => e.price)
	.sort((a, b) => -(a - b))
	[0];
</script>

<template>
	<tr>
		<td>{{ system.name }}</td>
		<td>
			<UnitValue v-if="sellPrice" :unit-id="133" :value="sellPrice" />
			<template v-else>None</template>
		</td>
		<td>
			<UnitValue v-if="sellPrice" :unit-id="133" :value="buyPrice" />
			<template v-else>None</template>
		</td>
	</tr>
</template>
