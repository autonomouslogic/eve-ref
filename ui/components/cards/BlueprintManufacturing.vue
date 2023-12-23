<script setup lang="ts">
import {Blueprint, BlueprintActivity, DogmaAttribute, InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import refdataApi from "~/refdata";
import BlueprintManufacturingLinks from "~/components/cards/BlueprintManufacturingLinks.vue";

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
			<table>
				<tr>
					<td>Manufacturing time:</td>
					<td>{{ manufacturing.time }}s</td>
				</tr>
				<tr>
					<td colspan="2">Products</td>
				</tr>
				<template v-if="manufacturing.products">
					<tr v-for="(product, productId) in manufacturing.products" :key="productId">
						<td><TypeLink :type-id="product.typeId" /></td>
						<td><FormattedNumber :number="product.quantity" /></td>
					</tr>
				</template>
				<template v-if="manufacturing.materials">
					<tr>
						<td colspan="2">Ingredients</td>
					</tr>
					<tr v-for="(material, materialId) in manufacturing.materials" :key="materialId">
						<td><TypeLink :type-id="material.typeId" /></td>
						<td><FormattedNumber :number="material.quantity" /></td>
					</tr>
				</template>
			</table>
			<BlueprintManufacturingLinks v-if="blueprint" :blueprint="blueprint" />
		</CardWrapper>
	</template>
</template>
