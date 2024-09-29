<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import DogmaListItems from "~/components/cards/DogmaListItems.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import UnitValue from "~/components/dogma/UnitValue.vue";
import AttributeList from "~/components/attr/AttributeList.vue";
import AttributeListItem from "~/components/attr/AttributeListItem.vue";
import {CUBIC_METER, KILOGRAM, MONEY} from "~/lib/unitConstants";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();
</script>

<template>
	<CardWrapper :title="title">

		<AttributeList :elements="4 + dogmaAttributes.length">
			<AttributeListItem v-if="inventoryType.basePrice">
				<template v-slot:key>Base price:</template>
				<UnitValue :unit-id="MONEY" :value="inventoryType.basePrice" />
			</AttributeListItem>
			<AttributeListItem v-if="inventoryType.mass">
				<template v-slot:key>Mass:</template>
				<UnitValue :unit-id="KILOGRAM" :value="inventoryType.mass" />
			</AttributeListItem>
			<AttributeListItem v-if="inventoryType.packagedVolume">
				<template v-slot:key>Packaged volume:</template>
				<UnitValue :unit-id="CUBIC_METER" :value="inventoryType.packagedVolume" />
			</AttributeListItem>
			<AttributeListItem v-if="inventoryType.portionSize">
				<template v-slot:key>Portion size:</template>
				<FormattedNumber :number="inventoryType.portionSize" />
			</AttributeListItem>

			<DogmaListItems :inventory-type="inventoryType" :dogma-attributes="dogmaAttributes" />

		</AttributeList>
	</CardWrapper>
</template>
