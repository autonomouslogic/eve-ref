<script setup lang="ts">
import {type Blueprint, type BlueprintActivity, type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import refdataApi from "~/refdata";
import BlueprintManufacturingLinks from "~/components/cards/BlueprintManufacturingLinks.vue";
import Duration from "~/components/dogma/units/Duration.vue";
import {secondsToMilliseconds} from "~/lib/timeUtils";
import AttributeListItem from "~/components/attr/AttributeListItem.vue";
import AttributeList from "~/components/attr/AttributeList.vue";

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

			<AttributeList>
				<AttributeListItem>
					<template v-slot:key>Reaction time:</template>
					<Duration :milliseconds="secondsToMilliseconds(reaction.time)" />
				</AttributeListItem>
			</AttributeList>

			<table v-if="reaction.products" class="standard-table">
				<thead>
					<th>Product</th>
					<th class="text-right">Quantity</th>
				</thead>
				<tbody>
					<tr v-for="(product, productId) in reaction.products" :key="productId">
						<td><TypeLink :type-id="product.typeId" /></td>
						<td class="text-right"><FormattedNumber :number="product.quantity" /></td>
					</tr>
				</tbody>
			</table>

			<table v-if="reaction.materials" class="standard-table">
				<thead>
					<th>Ingredient</th>
					<th class="text-right">Quantity</th>
				</thead>
				<tbody>
					<tr v-for="(material, materialId) in reaction.materials" :key="materialId">
						<td><TypeLink :type-id="material.typeId" /></td>
						<td class="text-right"><FormattedNumber :number="material.quantity" /></td>
					</tr>
				</tbody>
			</table>

			<BlueprintManufacturingLinks v-if="blueprint" :blueprint="blueprint" />
		</CardWrapper>
	</template>
</template>
