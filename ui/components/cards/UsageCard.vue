<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import DogmaListItems from "~/components/cards/DogmaListItems.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import UnitValue from "~/components/dogma/UnitValue.vue";
import AttributeList from "~/components/attr/AttributeList.vue";
import AttributeListItem from "~/components/attr/AttributeListItem.vue";
import {CUBIC_METER, KILOGRAM} from "~/lib/unitConstants";
import TypeLink from "~/components/helpers/TypeLink.vue";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

const canBeFittedWithTypes = computed(() => props.inventoryType.canBeFittedWithTypes || []);
const canBeFittedWithCount = computed(() => canBeFittedWithTypes.value.length || 0);

</script>

<template>
	<CardWrapper v-if="canBeFittedWithCount + dogmaAttributes.length > 0" :title="title">
		<AttributeList :elements="canBeFittedWithCount + dogmaAttributes.length">
			<AttributeListItem v-for="typeId in canBeFittedWithTypes" :key="typeId">
				<template v-slot:key>Can be fitted with:</template>
				<TypeLink :type-id="typeId" />
			</AttributeListItem>

			<DogmaListItems :inventory-type="inventoryType" :dogma-attributes="dogmaAttributes" />

		</AttributeList>
	</CardWrapper>
</template>
