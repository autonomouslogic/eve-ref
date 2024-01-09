<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();
</script>

<template>
	<template v-if="inventoryType.typeMaterials">
		<CardWrapper :title="title">
			<table class="table-auto text-left w-full">
				<tbody>
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
			<div class="mt-2">Basic reprocessing, not accounting for skills and other bonuses.</div>
		</CardWrapper>
	</template>
</template>
