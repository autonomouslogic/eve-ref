<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import {getMainDogma} from "~/lib/mainDogmaAttributes";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

const variationTypeIds: number[] = [];
if (props.inventoryType.typeVariations) {
	for (const metaGroupId of Object.keys(props.inventoryType.typeVariations)) {
		props.inventoryType.typeVariations[metaGroupId].forEach((typeId) => {
			variationTypeIds.push(typeId);
		});
	}
}

const comparisonDogmaAttributes = await getMainDogma(props.inventoryType);
</script>

<template>
	<template v-if="inventoryType.typeVariations">
		<CardWrapper :title="title">
			<CompareTable
				:type-ids="variationTypeIds"
				:current-type-id="inventoryType.typeId"
				:dogma-attribute-names="comparisonDogmaAttributes"
				direction="vertical"
				:compact-attribute-names="true"
				:show-meta-group="true"
				:load-bundles="false" />
		</CardWrapper>
	</template>
</template>
