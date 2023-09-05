<script setup lang="ts">
import {DogmaAttribute, InventoryType} from "~/refdata-openapi";

const props = defineProps<{
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

function value(attributeId: number | undefined) {
	if (!props.inventoryType.dogmaAttributes || !attributeId) {
		return undefined;
	}

	return props.inventoryType.dogmaAttributes[attributeId].value;
}
</script>

<template>
	<li v-for="attribute in dogmaAttributes" :key="attribute.attributeId">
		<DogmaAttributeValue
			:value="value(attribute.attributeId)"
			:attribute="attribute" />
	</li>
</template>
