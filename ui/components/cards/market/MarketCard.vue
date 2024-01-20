<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import MarketLinks from "~/components/cards/market/MarketLinks.vue";
import {HUB_STATION_IDS} from "~/lib/marketUtils";
import MarketRow from "~/components/cards/market/MarketRow.vue";

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
			<table class="table table-auto w-full">
				<thead>
					<tr>
						<th class="text-left">Hub</th>
						<th class="text-right">Sell</th>
						<th class="text-right">Buy</th>
					</tr>
				</thead>
				<tbody>
					<ClientOnly>
						<MarketRow v-for="stationId in stationIds" :key="stationId" :type-id="typeId" :station-id="stationId" />
					</ClientOnly>
				</tbody>
			</table>
			<MarketLinks :inventory-type="inventoryType" />
		</CardWrapper>
	</template>
</template>
