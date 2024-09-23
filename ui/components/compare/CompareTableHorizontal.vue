<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import TypeLink from "~/components/helpers/TypeLink.vue";
import DogmaAttributeLink from "~/components/helpers/DogmaAttributeLink.vue";
import AttributeTypeIcon from "~/components/icons/AttributeTypeIcon.vue";
import MetaGroupName from "~/components/helpers/MetaGroupName.vue";

export interface Props {
	inventoryTypes: InventoryType[],
	dogmaAttributes: DogmaAttribute[],
	compactAttributeNames: boolean,
	showMetaGroup: boolean
}
const props = withDefaults(defineProps<Props>(), {
});
</script>

<template>
	<thead>
		<th>Attribute</th>
		<th v-for="type in inventoryTypes" :key="type.typeId" class="text-right">
			<h2><type-link :type-id="type.typeId" /></h2>
		</th>
	</thead>
	<tbody>
		<tr v-if="showMetaGroup">
			<td><template v-if="!compactAttributeNames">Meta group</template></td>
			<td v-for="type in inventoryTypes" :key="type.typeId" class="text-right px-6">
				<MetaGroupName :meta-group-id="type.metaGroupId" />
			</td>
		</tr>
		<template v-for="attr in dogmaAttributes" :key="attr.attributeId">
			<tr v-if="attr && attr.attributeId">
				<td>
					<AttributeTypeIcon :dogma-attribute="attr" :size="25" />
					<DogmaAttributeLink v-if="!compactAttributeNames" :attribute="attr" />
				</td>
				<td v-for="type in inventoryTypes" :key="type.typeId" class="text-right px-6">
					<CompareTableCell :dogma-attribute="attr" :inventory-type="type" />
				</td>
			</tr>
		</template>
	</tbody>
</template>

<style scoped>
</style>
