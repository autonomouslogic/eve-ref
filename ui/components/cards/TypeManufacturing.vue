<script setup lang="ts">
import {Blueprint, BlueprintActivity, DogmaAttribute, InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import refdataApi from "~/refdata";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

let blueprintType: InventoryType | undefined;
if (!props.inventoryType.isBlueprint && props.inventoryType.producedByBlueprints) {
	const blueprintTypeId = Object.values(props.inventoryType.producedByBlueprints)
		.filter(b => b.blueprintActivity == "manufacturing")
		.map(b => b.blueprintTypeId)
		.shift();
	if (blueprintTypeId) {
		blueprintType = await refdataApi.getType({typeId: blueprintTypeId});
	}
}

let blueprint: Blueprint | undefined;
let copying: BlueprintActivity | undefined;
let invention: BlueprintActivity | undefined;
let manufacturing: BlueprintActivity | undefined;
let researchMaterial: BlueprintActivity | undefined;
let researchTime: BlueprintActivity | undefined;
if (blueprintType?.typeId) {
	blueprint = await refdataApi.getBlueprint({blueprintTypeId: blueprintType.typeId});
	const activities = blueprint.activities;
	if (activities) {
		copying = activities.copying;
		invention = activities.invention;
		manufacturing = activities.manufacturing;
		researchMaterial = activities.research_material;
		researchTime = activities.research_time;
	}
}

</script>

<template>
	<template v-if="blueprintType && blueprint">
		<CardWrapper :title="title">
			<ul>
				<li>Blueprint: <TypeLink :type-id="blueprintType.typeId" /></li>
				<li v-if="manufacturing">Manufacturing time: {{ manufacturing.time }}s</li>
				<template v-if="manufacturing">
					<li v-for="(product, id) in manufacturing.products" :key="id">
						{{ product.quantity }}x <TypeLink :type-id="product.typeId" />
					</li>
				</template>
				<li v-if="copying">Copy time: {{ copying.time }}s</li>
				<template v-if="invention">
					<li v-for="(product, id) in invention.products" :key="id">
						Invention: <TypeLink :type-id="product.typeId" />
					</li>
				</template>
				<li v-if="researchMaterial">Material research: {{ researchMaterial.time }}s</li>
				<li v-if="researchTime">Time research: {{ researchTime.time }}s</li>
			</ul>
		</CardWrapper>
	</template>
</template>
