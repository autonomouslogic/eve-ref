<script setup lang="ts">
import {DogmaAttribute, InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import MarketRow from "~/components/cards/market/MarketRow.vue";
import MarketLinks from "~/components/cards/market/MarketLinks.vue";
import {HUB_STATION_IDS} from "~/lib/marketUtils";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

const stationIds = HUB_STATION_IDS.values();

const typeId = computed(() => {
	const t = props.inventoryType.typeId;
	if (!t) {
		throw new Error("typeId is required");
	}
	return t;
});
</script>

<template>
	<template v-if="inventoryType.marketGroupId">
		<CardWrapper :title="title">
			<table>
				<thead>
					<tr>
						<th>Hub</th>
						<th>Sell</th>
						<th>Buy</th>
					</tr>
				</thead>
				<tbody>
					<MarketRow v-for="stationId in stationIds" :key="stationId" :type-id="typeId" :station-id="stationId" />
				</tbody>
			</table>
			<MarketLinks :inventory-type="inventoryType" />
		</CardWrapper>
	</template>
</template>
