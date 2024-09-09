<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import DogmaListItems from "~/components/cards/DogmaListItems.vue";
import AttributeListItem from "~/components/attr/AttributeListItem.vue";
import AttributeList from "~/components/attr/AttributeList.vue";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();
</script>

<template>
	<template v-if="inventoryType.typeMaterials">
		<CardWrapper :title="title">
			<AttributeList>
				<DogmaListItems :inventory-type="inventoryType" :dogma-attributes="dogmaAttributes" />
				<AttributeListItem v-if="inventoryType.portionSize">
					<template v-slot:key>Portion size:</template>
					<FormattedNumber :number="inventoryType.portionSize" />
				</AttributeListItem>
			</AttributeList>

			<table class="table-auto text-left w-full">
				<tbody>
					<tr>
						<td colspan="2">Output</td>
					</tr>
					<tr v-for="(material, materialId) in inventoryType.typeMaterials" :key="materialId">
						<td>
							<TypeLink :type-id="materialId" />
						</td>
						<td class="text-right">
							<FormattedNumber :number="material.quantity" />
						</td>
					</tr>
				</tbody>
			</table>
			<div class="mt-2 italic">Basic reprocessing, not accounting for skills and other bonuses.</div>
		</CardWrapper>
	</template>
</template>
