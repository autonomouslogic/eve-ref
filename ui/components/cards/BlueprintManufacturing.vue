<script setup lang="ts">
import {type Blueprint, type BlueprintActivity, type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import refdataApi from "~/refdata";
import BlueprintManufacturingLinks from "~/components/cards/BlueprintManufacturingLinks.vue";
import Duration from "~/components/dogma/units/Duration.vue";
import {secondsToMilliseconds} from "~/lib/timeUtils";
import AttributeList from "~/components/attr/AttributeList.vue";
import AttributeListItem from "~/components/attr/AttributeListItem.vue";

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
			<AttributeList>
				<AttributeListItem>
					<template v-slot:key>Manufacturing time:</template>
					<Duration :milliseconds="secondsToMilliseconds(manufacturing.time)" />
				</AttributeListItem>
			</AttributeList>

			<table v-if="manufacturing.products" class="standard-table">
				<thead>
					<tr>
						<th>Product</th>
						<th class="text-right">Quantity</th>
					</tr>
				</thead>
				<tbody>
					<tr v-for="(product, productId) in manufacturing.products" :key="productId">
						<td><TypeLink :type-id="product.typeId" /></td>
						<td class="text-right"><FormattedNumber :number="product.quantity" /></td>
					</tr>
				</tbody>
			</table>

			<table v-if="manufacturing.materials" class="standard-table">
				<thead>
					<tr>
						<th>Material</th>
						<th class="text-right">Quantity</th>
					</tr>
				</thead>
				<tbody>
					<tr v-for="(material, materialId) in manufacturing.materials" :key="materialId">
						<td><TypeLink :type-id="material.typeId" /></td>
						<td class="text-right"><FormattedNumber :number="material.quantity" /></td>
					</tr>
				</tbody>
			</table>

			<BlueprintManufacturingLinks v-if="blueprint" :blueprint="blueprint" />
		</CardWrapper>
	</template>
</template>
