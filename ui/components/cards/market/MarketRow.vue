<script setup lang="ts">
import {universeApi} from "~/esi";
import {GetMarketsRegionIdOrdersOrderTypeEnum} from "~/esi-openapi";
import UnitValue from "~/components/dogma/UnitValue.vue";
import {getOrders} from "~/lib/marketUtils";

const props = defineProps<{
	typeId: number,
	stationId: number
}>();

const station = await universeApi.getUniverseStationsStationId({ stationId: props.stationId });
const system = await universeApi.getUniverseSystemsSystemId({systemId: station.systemId});
const constellation = await universeApi.getUniverseConstellationsConstellationId({constellationId: system.constellationId});

var sellOrders = await getOrders(GetMarketsRegionIdOrdersOrderTypeEnum.Sell, props.typeId, constellation.regionId);
var buyOrders = await getOrders(GetMarketsRegionIdOrdersOrderTypeEnum.Buy, props.typeId, constellation.regionId);

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
