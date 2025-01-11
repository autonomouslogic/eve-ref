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
			<AttributeList>
				<AttrAttributeListItem>
					<template v-slot:key>Blueprint:</template>
					<TypeLink :type-id="blueprintType.typeId" />
				</AttrAttributeListItem>
			</AttributeList>

			<template v-if="manufacturing">
				<AttrAttributeListItem v-for="(product, id) in manufacturing.products" :key="id">
					<template v-slot:key>Output <TypeLink :type-id="product.typeId" />:</template>
					<FormattedNumber :number="product.quantity" />
				</AttrAttributeListItem>
				<AttrAttributeListItem>
					<template v-slot:key>Manufacturing time:</template>
					<Duration :milliseconds="secondsToMilliseconds(manufacturing.time)" />
				</AttrAttributeListItem>
			</template>
			<AttrAttributeListItem v-if="copying">
				<template v-slot:key>Copy time:</template>
				<Duration :milliseconds="secondsToMilliseconds(copying.time)" />
			</AttrAttributeListItem>
			<AttrAttributeListItem v-if="researchMaterial">
				<template v-slot:key>Material research:</template>
				<Duration :milliseconds="secondsToMilliseconds(researchMaterial.time)" />
			</AttrAttributeListItem>
			<AttrAttributeListItem v-if="researchTime">
				<template v-slot:key>Time research:</template>
				<Duration :milliseconds="secondsToMilliseconds(researchTime.time)" />
			</AttrAttributeListItem>

			<div class="mt-2">
				<BlueprintManufacturingLinks v-if="blueprint" :blueprint="blueprint" />
			</div>
		</CardWrapper>
	</template>
</template>
