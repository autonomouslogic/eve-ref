<script setup lang="ts">
import {type Blueprint, type BlueprintActivity, type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import refdataApi from "~/refdata";
import BlueprintManufacturingLinks from "~/components/cards/BlueprintManufacturingLinks.vue";
import Duration from "~/components/dogma/units/Duration.vue";

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
					<td class="text-right"><TypeLink :type-id="blueprintType.typeId" /></td>
				</tr>
				<template v-if="manufacturing">
					<tr v-for="(product, id) in manufacturing.products" :key="id">
						<td><TypeLink :type-id="product.typeId" /></td>
						<td class="text-right"><FormattedNumber :number="product.quantity" /></td>
					</tr>
					<tr>
						<td>Manufacturing time:</td>
						<td class="text-right"><Duration :milliseconds="manufacturing.time" /></td>
					</tr>
				</template>
				<tr v-if="copying">
					<td>Copy time:</td>
					<td class="text-right"><Duration :milliseconds="copying.time" /></td>
				</tr>
				<tr v-if="researchMaterial">
					<td>Material research:</td>
					<td class="text-right"><Duration :milliseconds="researchMaterial.time" /></td>
				</tr>
				<tr v-if="researchTime">
					<td>Time research:</td>
					<td class="text-right"><Duration :milliseconds="researchTime.time" /></td>
				</tr>
			</table>
			<div class="mt-1">
				<BlueprintManufacturingLinks v-if="blueprint" :blueprint="blueprint" />
			</div>
		</CardWrapper>
	</template>
</template>
