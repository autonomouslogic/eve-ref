<script setup lang="ts">
import {HUB_STATION_IDS} from "~/lib/marketUtils";
import MarketRow from "~/components/cards/market/MarketRow.vue";

const props = defineProps<{
	typeId: number
}>();

const stationIds = HUB_STATION_IDS.values();
</script>

<template>
	<table class="table table-auto w-full">
		<thead>
			<tr>
				<th class="text-left">Hub</th>
				<th class="text-right">Sell</th>
				<th class="text-right">Buy</th>
			</tr>
		</thead>
		<tbody>
			<MarketRow v-for="stationId in stationIds" :key="stationId" :type-id="typeId" :station-id="stationId" />
		</tbody>
	</table>
</template>
