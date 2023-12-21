<script setup lang="ts">
import {DogmaAttribute, DogmaTypeAttribute, InventoryType, MetaGroup} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import refdataApi from "~/refdata";
import TypeLink from "~/components/helpers/TypeLink.vue";
import {getAttributeByName, loadDogmaAttributesForType} from "~/lib/dogmaUtils";
import DogmaAttributeLink from "~/components/helpers/DogmaAttributeLink.vue";
import MarketGroupName from "~/components/helpers/MarketGroupName.vue";
import AttributeTypeIcon from "~/components/icons/AttributeTypeIcon.vue";

const {locale} = useI18n();

const props = defineProps<{
	typeIds: number[],
	dogmaAttributeNames: string[]
}>();

const types = await Promise.all(props.typeIds.map(typeId => refdataApi.getType({typeId})));
const dogmaAttributes: { [key: string]: DogmaTypeAttribute } = {};
for (let type of types) {
	const attrs = await loadDogmaAttributesForType(type);
	for (let k in attrs) {
		dogmaAttributes[k] = attrs[k];
	}
}
const dogmaAttributesArray = Object.values(dogmaAttributes);
const listAttributes = props.dogmaAttributeNames.map(name => getAttributeByName(name, dogmaAttributesArray));
</script>

<template>
	<table class="table-auto auto text-left">
		<thead>
			<th></th>
			<th v-for="type in types" :key="type.typeId" class="text-right px-6">
				<h2><type-link :type-id="type.typeId" /></h2>
			</th>
		</thead>
		<tbody>
			<template v-for="(attr, index) in listAttributes" :key="index">
				<tr v-if="attr && attr.attributeId" class="border-b">
					<td class="px-6">
						<AttributeTypeIcon :dogma-attribute="attr" :size="25" />
						<DogmaAttributeLink :attribute="attr" />
					</td>
					<td v-for="type in types" :key="type.typeId" class="text-right px-6">
						<template v-if="type.dogmaAttributes && type.dogmaAttributes[attr.attributeId] && type.dogmaAttributes[attr.attributeId].value">
							<dogma-value :value="type.dogmaAttributes[attr.attributeId].value!" :attribute="attr" />
						</template>
					</td>
				</tr>
			</template>
		</tbody>
	</table>
</template>
