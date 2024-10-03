<script setup lang="ts">
import {GetMarketsRegionIdOrdersOrderTypeEnum,} from "~/esi-openapi";
import UnitValue from "~/components/dogma/UnitValue.vue";
import {getOrders, type HubStation} from "~/lib/marketUtils";
import {MONEY} from "~/lib/unitConstants";

const props = defineProps<{
	typeId: number,
	hubStation: HubStation
}>();

const	sellOrders = await getOrders(GetMarketsRegionIdOrdersOrderTypeEnum.Sell, props.typeId, props.hubStation.regionId);
const	buyOrders = await getOrders(GetMarketsRegionIdOrdersOrderTypeEnum.Buy, props.typeId, props.hubStation.regionId);

const sellPrice = !sellOrders ? undefined : sellOrders.filter(e => e.locationId == props.hubStation.stationId)
	.map(e => e.price)
	.sort((a, b) => a - b)
	[0];
const buyPrice = !buyOrders ? undefined : buyOrders.filter(e => e.locationId == props.hubStation.stationId)
	.map(e => e.price)
	.sort((a, b) => -(a - b))
	[0];

</script>

<template>
	<tr>
		<td>{{ hubStation.systemName }}</td>
		<td class="text-right">
			<UnitValue v-if="sellPrice" :unit-id="MONEY" :value="sellPrice" />
			<span v-else>-</span>
		</td>
		<td class="text-right">
			<UnitValue v-if="buyPrice" :unit-id="MONEY" :value="buyPrice" />
			<span v-else>-</span>
		</td>
	</tr>
</template>
