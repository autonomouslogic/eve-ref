<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import DogmaListItems from "~/components/cards/DogmaListItems.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import Duration from "~/components/dogma/units/Duration.vue";
import AttributeList from "~/components/attr/AttributeList.vue";
import AttributeListItem from "~/components/attr/AttributeListItem.vue";
import {getTypeAttributeByName} from "~/lib/dogmaUtils";
import {
	calculateAcceleratedSkillpointsAlpha,
	calculateAcceleratedSkillpointsOmega,
	calculateBoosterDuration
} from "~/lib/boosterUtils";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

const bonusAttrs = ["charismaBonus", "intelligenceBonus", "memoryBonus", "perceptionBonus", "willpowerBonus"];
const durationAttr = "boosterDuration";

const skillpoints = computed(() => {
	const bonus = getTypeAttributeByName(bonusAttrs[0], props.inventoryType, props.dogmaAttributes);
	const duration = getTypeAttributeByName(durationAttr, props.inventoryType, props.dogmaAttributes);
	if (bonus?.value === undefined || duration?.value === undefined) {
		return undefined;
	}
	const realDuration = calculateBoosterDuration(duration.value, 5);
	return [
		calculateAcceleratedSkillpointsAlpha(bonus.value, realDuration),
		calculateAcceleratedSkillpointsOmega(bonus.value, realDuration)
	];
});

const duration = computed(() => {
	const duration = getTypeAttributeByName(durationAttr, props.inventoryType, props.dogmaAttributes);
	if (duration?.value === undefined) {
		return undefined;
	}
	return calculateBoosterDuration(duration.value, 5);
});
</script>

<template>
	<CardWrapper :title="title">
		<AttributeList>
			<template v-if="skillpoints !== undefined">
				<AttributeListItem>
					<template v-slot:key>Accelerated Skillpoints (alpha, Biology V):</template>
					<FormattedNumber :number="skillpoints[0]" />
				</AttributeListItem>
				<AttributeListItem>
					<template v-slot:key>Accelerated Skillpoints (omega, Biology V):</template>
					<FormattedNumber :number="skillpoints[1]" />
				</AttributeListItem>
				<AttributeListItem v-if="duration">
					<template v-slot:key>Booster Duration (Biology V):</template>
					<Duration :milliseconds="duration" />
				</AttributeListItem>
			</template>

			<DogmaListItems :inventory-type="inventoryType" :dogma-attributes="dogmaAttributes" />

		</AttributeList>
	</CardWrapper>
</template>
