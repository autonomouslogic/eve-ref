<script setup lang="ts">
import {HUB_STATION_IDS} from "~/lib/marketUtils";
import MarketRow from "~/components/cards/market/MarketRow.vue";

const props = defineProps<{
	typeId: number
}>();

const stationIds = HUB_STATION_IDS.values();
</script>

<template>
	<table class="standard-table">
		<thead>
			<tr>
				<th>Hub</th>
				<th class="text-right">Sell</th>
				<th class="text-right">Buy</th>
			</tr>
		</thead>
		<tbody>
			<MarketRow v-for="stationId in stationIds" :key="stationId" :type-id="typeId" :station-id="stationId" />
		</tbody>
	</table>
</template>
