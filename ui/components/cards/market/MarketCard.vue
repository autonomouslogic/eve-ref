<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import MarketLinks from "~/components/cards/market/MarketLinks.vue";
import MarketCardInner from "~/components/cards/market/MarketCardInner.vue";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();
</script>

<template>
	<template v-if="inventoryType.typeId && inventoryType.marketGroupId">
		<CardWrapper :title="title">
			<ClientOnly>
				<MarketCardInner :inventory-type="inventoryType" :type-id="inventoryType.typeId" />
			</ClientOnly>
			<MarketLinks :inventory-type="inventoryType" />
		</CardWrapper>
	</template>
</template>
