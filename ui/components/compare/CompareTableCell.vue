<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";

export interface Props {
	inventoryType: InventoryType,
	dogmaAttribute: DogmaAttribute,
}
const props = withDefaults(defineProps<Props>(), {
});

function hasValue(attr: DogmaAttribute, type: InventoryType): boolean {
	return !!(attr?.attributeId && type?.dogmaAttributes?.[attr.attributeId]?.value);
}

function getValue(attr: DogmaAttribute, type: InventoryType): number {
	const val = attr?.attributeId ? type?.dogmaAttributes?.[attr?.attributeId]?.value : undefined;
	return val === undefined ? -1 : val;
}
</script>

<template>
	<template v-if="hasValue(dogmaAttribute, inventoryType)">
		<DogmaValue :value="getValue(dogmaAttribute, inventoryType)" :attribute="dogmaAttribute" />
	</template>
</template>
