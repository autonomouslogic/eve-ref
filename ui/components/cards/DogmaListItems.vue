<script setup lang="ts">
import {DogmaAttribute, InventoryType} from "~/refdata-openapi";

const props = defineProps<{
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

function value(attributeId: number | undefined): number {
	if (!props.inventoryType.dogmaAttributes || !attributeId) {
		throw new Error(`No dogma attributes for inventory type ${props.inventoryType.typeId}`);
	}

	const attributeValue: number | undefined = props.inventoryType.dogmaAttributes[attributeId].value;
	if (attributeValue === undefined) {
		throw new Error(`No value for attribute ${attributeId} on inventory type ${props.inventoryType.typeId}`);
	}

	return attributeValue;
}
</script>

<template>
	<template v-for="attribute in dogmaAttributes" :key="attribute.attributeId">
		<DogmaAttributeValue
			:value="value(attribute.attributeId)"
			:attribute="attribute" />
	</template>
</template>
