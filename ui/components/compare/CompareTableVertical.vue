<script setup lang="ts">
import {type DogmaAttribute, type DogmaTypeAttribute, type InventoryType} from "~/refdata-openapi";
import refdataApi, {cacheBundle} from "~/refdata";
import TypeLink from "~/components/helpers/TypeLink.vue";
import {getAttributeByName, loadDogmaAttributesForType} from "~/lib/dogmaUtils";
import DogmaAttributeLink from "~/components/helpers/DogmaAttributeLink.vue";
import AttributeTypeIcon from "~/components/icons/AttributeTypeIcon.vue";

export interface Props {
	inventoryTypes: InventoryType[],
	dogmaAttributes: DogmaAttribute[],
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
	<thead>
		<th></th>
		<template v-for="attr in dogmaAttributes" :key="attr.attributeId">
			<th v-if="attr && attr.attributeId" class="px-6">
				<td class="px-6">
					<AttributeTypeIcon :dogma-attribute="attr" :size="25" />
					<DogmaAttributeLink :attribute="attr" />
				</td>
			</th>
		</template>
	</thead>
	<tbody>
		<tr v-for="type in inventoryTypes" :key="type.typeId" class="border-b border-gray-700">
			<td class="text-left"><type-link :type-id="type.typeId" /></td>
			<td v-for="attr in dogmaAttributes" :key="attr.attributeId" class="text-right">
				<CompareTableCell :dogma-attribute="attr" :inventory-type="type" />
			</td>
		</tr>
	</tbody>
</template>
