<script setup lang="ts">
import {type DogmaAttribute, type DogmaTypeAttribute, type InventoryType} from "~/refdata-openapi";
import refdataApi, {cacheBundle} from "~/refdata";
import TypeLink from "~/components/helpers/TypeLink.vue";
import {getAttributeByName, loadDogmaAttributesForType} from "~/lib/dogmaUtils";
import DogmaAttributeLink from "~/components/helpers/DogmaAttributeLink.vue";
import AttributeTypeIcon from "~/components/icons/AttributeTypeIcon.vue";

export interface Props {
	typeIds: number[],
	dogmaAttributeNames: string[],
	direction: "vertical" | "horizontal"
}
const props = withDefaults(defineProps<Props>(), {
	direction: "horizontal"
});

const types: InventoryType[] = await Promise.all(props.typeIds.map(typeId => {
	return cacheBundle(typeId).then(() => {
		return refdataApi.getType({typeId});
	});
	return refdataApi.getType({typeId});
}));
const dogmaAttributes: { [key: string]: DogmaTypeAttribute } = {};
for (let type of types) {
	const attrs = await loadDogmaAttributesForType(type);
	for (let k in attrs) {
		dogmaAttributes[k] = attrs[k];
	}
}
const dogmaAttributesArray = Object.values(dogmaAttributes);
const listAttributes: DogmaAttribute[] = props.dogmaAttributeNames
	.map(name => getAttributeByName(name, dogmaAttributesArray))
	.filter(attr => attr !== undefined) as DogmaAttribute[];

function hasValue(attr: DogmaAttribute, type: InventoryType): boolean {
	return !!(attr?.attributeId && type?.dogmaAttributes?.[attr.attributeId]?.value);
}

function getValue(attr: DogmaAttribute, type: InventoryType): number {
	const val = attr?.attributeId ? type?.dogmaAttributes?.[attr?.attributeId]?.value : undefined;
	return val === undefined ? -1 : val;
}
</script>

<template>
	<table class="table-auto text-left">
		<CompareTableHorizontal v-if="direction == 'horizontal'" :inventory-types="types" :dogma-attributes="listAttributes" />
		<CompareTableVertical v-if="direction == 'vertical'" :inventory-types="types" :dogma-attributes="listAttributes" />
	</table>
</template>
