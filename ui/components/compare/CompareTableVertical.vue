<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import TypeLink from "~/components/helpers/TypeLink.vue";
import DogmaAttributeLink from "~/components/helpers/DogmaAttributeLink.vue";
import AttributeTypeIcon from "~/components/icons/AttributeTypeIcon.vue";
import MetaGroupName from "~/components/helpers/MetaGroupName.vue";
import MarketPrice from "~/components/helpers/MarketPrice.vue";

export interface Props {
	inventoryTypes: InventoryType[],
	dogmaAttributes: DogmaAttribute[],
	compactAttributeNames: boolean,
	showMarketPrice: boolean,
	showMetaGroup: boolean
}
const props = withDefaults(defineProps<Props>(), {
});
</script>

<template>
	<thead>
		<tr>
			<th class="text-left">Type</th>
			<th v-if="showMarketPrice" class="text-right">
				<template v-if="!compactAttributeNames">Price</template>
			</th>
			<th v-if="showMetaGroup">
				<template v-if="!compactAttributeNames">Meta group</template>
			</th>
			<template v-for="attr in dogmaAttributes" :key="attr.attributeId">
				<th v-if="attr && attr.attributeId" class="text-right">
					<AttributeTypeIcon :dogma-attribute="attr" :size="25" />
					<DogmaAttributeLink v-if="!compactAttributeNames" :attribute="attr" />
				</th>
			</template>
		</tr>
	</thead>
	<tbody>
		<tr v-for="type in inventoryTypes" :key="type.typeId" class="border-b border-gray-700">
			<td class="text-left"><type-link :type-id="type.typeId" /></td>
			<td v-if="showMarketPrice" class="text-right">
				<MarketPrice v-if="type.typeId" :type-id="type.typeId" />
			</td>
			<td v-if="showMetaGroup">
				<MetaGroupName :meta-group-id="type.metaGroupId" />
			</td>
			<td v-for="attr in dogmaAttributes" :key="attr.attributeId" class="text-right">
				<CompareTableCell :dogma-attribute="attr" :inventory-type="type" />
			</td>
		</tr>
	</tbody>
</template>

<style scoped>
</style>
