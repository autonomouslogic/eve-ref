<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import DogmaListItems from "~/components/cards/DogmaListItems.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import MarketGroupLink from "~/components/helpers/MarketGroupLink.vue";
import UnitValue from "~/components/dogma/UnitValue.vue";
import GroupLink from "~/components/helpers/GroupLink.vue";
import AttributeList from "~/components/attr/AttributeList.vue";
import AttributeListItem from "~/components/attr/AttributeListItem.vue";
import {CUBIC_METER, KILOGRAM, METER, MONEY} from "~/lib/unitConstants";
import {DateTime} from "luxon";
import {getAttributeByName, getTypeAttributeByName} from "~/lib/dogmaUtils";
import {MINUTE} from "~/lib/timeUtils";

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
	return bonus.value *1.5 * duration.value / MINUTE;
});
</script>

<template>
	<CardWrapper :title="title">
		<AttributeList>
			<template v-if="skillpoints !== undefined">
				<AttributeListItem>
					<template v-slot:key>Accelerated Skillpoints (alpha):</template>
					<FormattedNumber :number="skillpoints / 2" />
				</AttributeListItem>
				<AttributeListItem>
					<template v-slot:key>Accelerated Skillpoints (omega):</template>
					<FormattedNumber :number="skillpoints" />
				</AttributeListItem>
			</template>

			<DogmaListItems :inventory-type="inventoryType" :dogma-attributes="dogmaAttributes" />

		</AttributeList>
	</CardWrapper>
</template>
