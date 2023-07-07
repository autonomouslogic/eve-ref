<script setup lang="ts">
import {DogmaAttribute, InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import DogmaListItems from "~/components/cards/DogmaListItems.vue";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();
</script>

<template>
	<template v-if="dogmaAttributes && dogmaAttributes.length > 0">
		<CardWrapper :title="title">
			<ul class="list-disc list-inside">
				<DogmaListItems :inventory-type="inventoryType" :dogma-attributes="dogmaAttributes" />
			</ul>
		</CardWrapper>
	</template>
</template>
