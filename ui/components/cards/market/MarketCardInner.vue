<script setup lang="ts">
import {HUB_STATIONS, PLEX_REGION_HUB_STATION} from "~/lib/marketUtils";
import MarketRow from "~/components/cards/market/MarketRow.vue";
import {PLEX_TYPE_ID} from "~/lib/typeConstants";

const props = defineProps<{
	typeId: number
}>();

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
			<template v-if="typeId == PLEX_TYPE_ID">
				<MarketRow :type-id="typeId" :hub-station="PLEX_REGION_HUB_STATION" />
			</template>
			<template v-else>
				<MarketRow v-for="hubStation in HUB_STATIONS.values()" :key="hubStation.stationId" :type-id="typeId" :hub-station="hubStation" />
			</template>
		</tbody>
	</table>
</template>
