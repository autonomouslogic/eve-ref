<script setup lang="ts">
import {type DogmaAttribute, type DogmaTypeAttribute, type InventoryType} from "~/refdata-openapi";
import refdataApi, {cacheTypeBundle} from "~/refdata";
import {getAttributeByName, loadDogmaAttributesForType} from "~/lib/dogmaUtils";

export interface Props {
	typeIds: number[],
	dogmaAttributeNames: string[],
	direction?: "vertical" | "horizontal",
	compactAttributeNames?: boolean,
	showMetaGroup?: boolean,
	loadBundles?: boolean,
}
const props = withDefaults(defineProps<Props>(), {
	direction: "horizontal",
	compactAttributeNames: false,
	showMetaGroup: false,
	loadBundles: true
});

const types: InventoryType[] = await Promise.all(props.typeIds.map(async typeId => {
	if (props.loadBundles) {
		await cacheTypeBundle(typeId);
	}
	return await refdataApi.getType({typeId});
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
	<table class="standard-table">
		<CompareTableHorizontal v-if="direction == 'horizontal'"
			:inventory-types="types"
			:dogma-attributes="listAttributes"
			:compact-attribute-names="compactAttributeNames"
			:show-meta-group="showMetaGroup"  />
		<CompareTableVertical v-if="direction == 'vertical'"
			:inventory-types="types"
			:dogma-attributes="listAttributes"
			:compact-attribute-names="compactAttributeNames"
			:show-meta-group="showMetaGroup" />
	</table>
</template>
