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
import ExternalLink from "~/components/helpers/ExternalLink.vue";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

const showCard = computed(() => {
	return !props.inventoryType.isBlueprint;
});
</script>

<template>
	<CardWrapper :title="title" v-if="showCard">
		<ExternalLink
			v-if="!inventoryType.isBlueprint"
			:url="`https://www.adam4eve.eu/material_influence.php?material=${inventoryType.typeId}`">Adam4Eve Material Influence</ExternalLink>
	</CardWrapper>
</template>
