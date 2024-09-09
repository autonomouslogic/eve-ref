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

const dogmaAttributes = await getMainDogma(props.inventoryType);
</script>

<template>
	<template v-if="inventoryType.typeVariations">
		<CardWrapper :title="title">
			<CompareTable
				:type-ids="variationTypeIds"
				:dogma-attribute-names="dogmaAttributes"
				direction="vertical"
				:compact-attribute-names="true"
				:show-meta-group="true"/>
		</CardWrapper>
	</template>
</template>
