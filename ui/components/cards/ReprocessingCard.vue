<script setup lang="ts">
import {DogmaAttribute, InventoryType} from "~/refdata-openapi";
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
			<ul>
				<li v-for="(material, materialId) in inventoryType.typeMaterials" :key="materialId">
					<TypeLink :type-id="materialId" /> <FormattedNumber :number="material.quantity" />
				</li>
			</ul>
			<div class="mt-2">Basic reprocessing, not accounting for skills and other bonuses.</div>
		</CardWrapper>
	</template>
</template>
