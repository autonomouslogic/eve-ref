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

</script>

<template>
	<template v-if="inventoryType.producedByBlueprints">
		<CardWrapper :title="title">
			<AttributeList>
				<AttrAttributeListItem v-for="(producedBy, id) in inventoryType.producedByBlueprints" :key="id">
					<template v-slot:key>Blueprint {{ producedBy.blueprintActivity }}:</template>
					<TypeLink :type-id="producedBy.blueprintTypeId" />
				</AttrAttributeListItem>
			</AttributeList>
		</CardWrapper>
	</template>
</template>
