<script setup lang="ts">
import {DogmaAttribute, InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import MarketRow from "~/components/cards/market/MarketRow.vue";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

const stationIds = [
	60003760, // Jita IV - Moon 4 - Caldari Navy Assembly Plant
	60008494, // Amarr VIII (Oris) - Emperor Family Academy
	60004588, // Rens VI - Moon 8 - Brutor Tribe Treasury
	60011866, // Dodixie IX - Moon 20 - Federation Navy Assembly Plant
	60005686, // Hek VIII - Moon 12 - Boundless Creation Factory
	60001804, // Maila VI - Moon 1 - Zainou Biotech Production
];
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
		</CardWrapper>
	</template>
</template>
