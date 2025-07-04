<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import TypeLink from "~/components/helpers/TypeLink.vue";
import DogmaAttributeLink from "~/components/helpers/DogmaAttributeLink.vue";
import AttributeTypeIcon from "~/components/icons/AttributeTypeIcon.vue";
import MarketPrice from "~/components/helpers/MarketPrice.vue";
import MetaGroupLink from "~/components/helpers/MetaGroupLink.vue";
import TypeName from "~/components/helpers/TypeName.vue";
import EveImage from "~/components/icons/EveImage.vue";

export interface Props {
	inventoryTypes: InventoryType[],
	currentTypeId?: number,
	dogmaAttributes: DogmaAttribute[],
	compactAttributeNames: boolean,
	showMarketPrice: boolean,
	showMetaGroup: boolean
}

const props = withDefaults(defineProps<Props>(), {});
</script>

<template>
	<thead>
		<tr>
			<th></th>
			<th v-for="type in inventoryTypes" :key="type.typeId" class="text-right">
				<h2>
					<TypeName v-if="type.typeId == currentTypeId" :type-id="type.typeId" />
					<TypeLink v-else :type-id="type.typeId" />
				</h2>
			</th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td>
				<template v-if="!compactAttributeNames">Icon</template>
			</td>
			<td v-for="type in inventoryTypes" :key="type.typeId" class="text-right px-6">
				<EveImage v-if="type.typeId" :type-id="type.typeId"/>
			</td>
		</tr>
		<tr v-if="showMarketPrice">
			<td>
				<template v-if="!compactAttributeNames">Price</template>
			</td>
			<td v-for="type in inventoryTypes" :key="type.typeId" class="text-right px-6">
				<MarketPrice v-if="type.typeId" :type-id="type.typeId"/>
			</td>
		</tr>
		<tr v-if="showMetaGroup">
			<td>
				<template v-if="!compactAttributeNames">Meta group</template>
			</td>
			<td v-for="type in inventoryTypes" :key="type.typeId" class="text-right px-6">
				<MetaGroupLink :meta-group-id="type.metaGroupId"/>
			</td>
		</tr>
		<template v-for="attr in dogmaAttributes" :key="attr.attributeId">
			<tr v-if="attr && attr.attributeId">
				<td>
					<AttributeTypeIcon :dogma-attribute="attr" :size="25"/>
					<DogmaAttributeLink v-if="!compactAttributeNames" :attribute="attr"/>
				</td>
				<td v-for="type in inventoryTypes" :key="type.typeId" class="text-right px-6">
					<CompareTableCell :dogma-attribute="attr" :inventory-type="type"/>
				</td>
			</tr>
		</template>
	</tbody>
</template>

<style scoped>
</style>
