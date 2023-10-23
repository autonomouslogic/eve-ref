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
			<table>
				<tr>
					<td>Blueprint:</td>
					<td><TypeLink :type-id="blueprintType.typeId" /></td>
				</tr>
				<template v-if="manufacturing">
					<tr v-for="(product, id) in manufacturing.products" :key="id">
						<td><TypeLink :type-id="product.typeId" /></td>
						<td><FormattedNumber :number="product.quantity" /></td>
					</tr>
					<tr>
						<td>Manufacturing time:</td>
						<td>{{ manufacturing.time }}s</td>
					</tr>
				</template>
				<tr v-if="copying">
					<td>Copy time:</td>
					<td>{{ copying.time }}s</td>
				</tr>
				<tr v-if="researchMaterial">
					<td>Material research:</td>
					<td>{{ researchMaterial.time }}s</td>
				</tr>
				<tr v-if="researchTime">
					<td>Time research:</td>
					<td>{{ researchTime.time }}s</td>
				</tr>
			</table>
		</CardWrapper>
	</template>
</template>
