<script setup lang="ts">
import {
	type Blueprint,
	type BlueprintActivity,
	type DogmaAttribute,
	type InventoryType,
	type Schematic
} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import refdataApi from "~/refdata";
import BlueprintManufacturingLinks from "~/components/cards/BlueprintManufacturingLinks.vue";
import Duration from "~/components/dogma/units/Duration.vue";
import {secondsToMilliseconds} from "~/lib/timeUtils";
import AttributeList from "~/components/attr/AttributeList.vue";
import DogmaListItems from "~/components/cards/DogmaListItems.vue";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

const producedBySchematics: Schematic[] = [];
if (props.inventoryType.producedBySchematicIds) {
	for (let schematicId of props.inventoryType.producedBySchematicIds) {
		const schematic = await refdataApi.getSchematic({schematicId});
		producedBySchematics.push(schematic);
	}
}
</script>

<template>
	<CardWrapper :title="title">
		<AttributeList>
			<DogmaListItems :inventory-type="inventoryType" :dogma-attributes="dogmaAttributes" />
		</AttributeList>

		<template v-if="producedBySchematics.length > 0">
        
			<template v-for="schematic in producedBySchematics" :key="schematic.schematicId">
				<table class="w-full">
					<tr v-if="schematic.name?.en">
						<td>Schematic</td>
						<td>{{ schematic.name.en }} [{{schematic.schematicId}}]</td>
					</tr>
					<tr v-if="schematic.cycleTime">
						<td>Cycle time</td>
						<td><Duration :milliseconds="schematic.cycleTime * 1000" /></td>
					</tr>
				</table>

				<table v-if="schematic.products" class="w-full mt-3">
					<thead>
						<tr>
							<th>Product</th>
							<th>Quantity</th>
						</tr>
					</thead>
					<tr v-for="(product, productId) in schematic.products" :key="productId">
						<td><TypeLink :type-id="product.typeId" /></td>
						<td><FormattedNumber :number="product.quantity" /></td>
					</tr>
				</table>

				<table v-if="schematic.materials" class="w-full mt-3">
					<thead>
						<tr>
							<th>Material</th>
							<th>Quantity</th>
						</tr>
					</thead>
					<tr v-for="(material, materialId) in schematic.materials" :key="materialId">
						<td><TypeLink :type-id="material.typeId" /></td>
						<td><FormattedNumber :number="material.quantity" /></td>
					</tr>
				</table>

				<div v-if="schematic.pinTypeIds && schematic.pinTypeIds.length > 0" class="mt-3">
					<b>Produced in:</b>
					<ul>
						<li v-for="pinTypeId in schematic.pinTypeIds" :key="pinTypeId">
							<TypeLink :type-id="pinTypeId" />
						</li>
					</ul>
				</div>

			</template>
		</template>
	</CardWrapper>
</template>

<style>
th {
  @apply text-left;
}
</style>
