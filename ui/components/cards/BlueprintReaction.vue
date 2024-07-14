<script setup lang="ts">
import {type Blueprint, type BlueprintActivity, type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import refdataApi from "~/refdata";
import BlueprintManufacturingLinks from "~/components/cards/BlueprintManufacturingLinks.vue";
import Duration from "~/components/dogma/units/Duration.vue";
import {secondsToMilliseconds} from "~/lib/timeUtils";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

let blueprintType: InventoryType | undefined = props.inventoryType.isBlueprint ? props.inventoryType : undefined;

let blueprint: Blueprint | undefined;
let reaction: BlueprintActivity | undefined;
if (blueprintType?.typeId) {
	blueprint = await refdataApi.getBlueprint({blueprintTypeId: blueprintType.typeId});
	const activities = blueprint.activities;
	if (activities) {
		reaction = activities.reaction;
	}
}

</script>

<template>
	<template v-if="reaction">
		<CardWrapper :title="title">
			<table>
				<tr>
					<td>Manufacturing time:</td>
					<td><Duration :milliseconds="secondsToMilliseconds(reaction.time)" /></td>
				</tr>
				<tr>
					<td colspan="2">Products</td>
				</tr>
				<template v-if="reaction.products">
					<tr v-for="(product, productId) in reaction.products" :key="productId">
						<td><TypeLink :type-id="product.typeId" /></td>
						<td><FormattedNumber :number="product.quantity" /></td>
					</tr>
				</template>
				<template v-if="reaction.materials">
					<tr>
						<td colspan="2">Ingredients</td>
					</tr>
					<tr v-for="(material, materialId) in reaction.materials" :key="materialId">
						<td><TypeLink :type-id="material.typeId" /></td>
						<td><FormattedNumber :number="material.quantity" /></td>
					</tr>
				</template>
			</table>
			<BlueprintManufacturingLinks v-if="blueprint" :blueprint="blueprint" />
		</CardWrapper>
	</template>
</template>
