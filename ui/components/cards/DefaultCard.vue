<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import DogmaListItems from "~/components/cards/DogmaListItems.vue";
import AttributeList from "~/components/attr/AttributeList.vue";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[],
	component: string | undefined
}>();
</script>

<template>
	<template v-if="(dogmaAttributes && dogmaAttributes.length > 0) || component">
		<CardWrapper :title="title">
			<div v-if="component" class="text-red-700 border border-red-700 p-2 text-center">Unknown component "{{component}}"</div>
			<AttributeList :elements="dogmaAttributes.length">
				<DogmaListItems :inventory-type="inventoryType" :dogma-attributes="dogmaAttributes" />
			</AttributeList>
		</CardWrapper>
	</template>
</template>
