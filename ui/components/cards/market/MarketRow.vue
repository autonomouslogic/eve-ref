<script setup lang="ts">
import {type HubStation} from "~/lib/marketUtils";
import MarketPrice from "~/components/helpers/MarketPrice.vue";

const props = defineProps<{
	typeId: number,
	hubStation: HubStation
}>();

</script>

<template>
	<tr>
		<td>{{ hubStation.systemName }}</td>
		<td class="text-right">
			<MarketPrice :type-id="typeId" order-type="sell" :region-id="hubStation.regionId" :station-id="hubStation.stationId" />
		</td>
		<td class="text-right">
			<MarketPrice :type-id="typeId" order-type="buy" :region-id="hubStation.regionId" :station-id="hubStation.stationId" />
		</td>
	</tr>
</template>
