<script setup lang="ts">
import {universeApi} from "~/esi";
import {GetMarketsRegionIdOrdersOrderTypeEnum,} from "~/esi-openapi";
import UnitValue from "~/components/dogma/UnitValue.vue";
import {getOrders} from "~/lib/marketUtils";
import {MONEY} from "~/lib/unitConstants";

const props = defineProps<{
	typeId: number,
	stationId: number
}>();

const station = await universeApi.getUniverseStationsStationId({ stationId: props.stationId });
const system = !station ? undefined :
	await universeApi.getUniverseSystemsSystemId({systemId: station.systemId});
const constellation = !system ? undefined :
	await universeApi.getUniverseConstellationsConstellationId({constellationId: system.constellationId});

const	sellOrders = !constellation ?
	undefined :
	await getOrders(GetMarketsRegionIdOrdersOrderTypeEnum.Sell, props.typeId, constellation.regionId);
const	buyOrders = !constellation ?
	undefined :
	await getOrders(GetMarketsRegionIdOrdersOrderTypeEnum.Buy, props.typeId, constellation.regionId);

const sellPrice = !sellOrders ? undefined : sellOrders.filter(e => e.locationId == station.stationId)
	.map(e => e.price)
	.sort((a, b) => a - b)
	[0];

const buyPrice = !buyOrders ? undefined : buyOrders.filter(e => e.locationId == station.stationId)
	.map(e => e.price)
	.sort((a, b) => -(a - b))
	[0];
</script>

<template>
	<tr v-if="system">
		<td class="text-left">{{ system.name }}</td>
		<td class="text-right">
			<UnitValue v-if="sellPrice" :unit-id="MONEY" :value="sellPrice" />
			<template v-else>None</template>
		</td>
		<td class="text-right">
			<UnitValue v-if="buyPrice" :unit-id="MONEY" :value="buyPrice" />
			<template v-else>None</template>
		</td>
	</tr>
</template>
