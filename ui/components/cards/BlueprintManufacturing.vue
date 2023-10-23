<script setup lang="ts">
import {Blueprint, BlueprintActivity, DogmaAttribute, InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import refdataApi from "~/refdata";
import {getJitaSellPrice} from "~/lib/marketUtils";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

let blueprintType: InventoryType | undefined = props.inventoryType.isBlueprint ? props.inventoryType : undefined;

let blueprint: Blueprint | undefined;
let manufacturing: BlueprintActivity | undefined;
if (blueprintType?.typeId) {
	blueprint = await refdataApi.getBlueprint({blueprintTypeId: blueprintType.typeId});
	const activities = blueprint.activities;
	if (activities) {
		manufacturing = activities.manufacturing;
	}
}

</script>

<template>
	<template v-if="manufacturing">
		<CardWrapper :title="title">
			<ul>
				<li>Manufacturing time: {{ manufacturing.time }}s</li>
				<li>
					Products:
					<div v-for="(product, id) in manufacturing.products" :key="id">
						{{ product.quantity }}x <TypeLink :type-id="product.typeId" />
					</div>
				</li>
				<li v-if="manufacturing.materials">
					Ingredients:
				<li v-for="(material, materialId) in manufacturing.materials" :key="materialId">
					<TypeLink :type-id="materialId" /> <FormattedNumber :number="material.quantity" />
				</li>
			</ul>
		</CardWrapper>
	</template>
</template>
