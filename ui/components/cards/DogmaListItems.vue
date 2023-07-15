<script setup lang="ts">
import {DogmaAttribute, InventoryType} from "~/refdata-openapi";

const props = defineProps<{
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();
</script>

<template>
	<li v-for="attribute in dogmaAttributes" :key="attribute.attributeId">
		<DogmaAttributeValue v-if="inventoryType.dogmaAttributes[attribute.attributeId]"
			:value="inventoryType.dogmaAttributes[attribute.attributeId].value"
			:attribute-id="attribute.attributeId" />
		<span v-else>No value for attribute {{attribute.attributeId}}</span>
	</li>
</template>
