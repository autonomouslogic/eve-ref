<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import refdataApi from "~/refdata";
import AttributeList from "~/components/attr/AttributeList.vue";
import AttributeListItem from "~/components/attr/AttributeListItem.vue";
import Duration from "~/components/dogma/units/Duration.vue";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

const blueprintType = props.inventoryType.isBlueprint ? props.inventoryType : undefined;
const blueprint = !blueprintType || !blueprintType?.typeId ? undefined :
	await refdataApi.getBlueprint({blueprintTypeId: blueprintType.typeId});
const copyingTime = blueprint?.activities?.copying?.time;
const productionLimit = blueprint?.maxProductionLimit;
</script>

<template>
	<template v-if="copyingTime != undefined || productionLimit !== undefined">
		<CardWrapper :title="title">
			<AttributeList>
				<AttributeListItem v-if="copyingTime != undefined">
					<template v-slot:key>Copy time:</template>
					<Duration :milliseconds="copyingTime * 1000" />
				</AttributeListItem>
				<AttributeListItem v-if="productionLimit != undefined">
					<template v-slot:key>Production limit:</template>
					<FormattedNumber :number="productionLimit" />
				</AttributeListItem>
			</AttributeList>
		</CardWrapper>
	</template>
</template>
