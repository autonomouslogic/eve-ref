<script setup lang="ts">
import {DogmaAttribute, InventoryType} from "~/refdata-openapi";
import DefaultCard from "~/components/cards/DefaultCard.vue";
import BasicCard from "~/components/cards/BasicCard.vue";
import ReprocessingCard from "~/components/cards/ReprocessingCard.vue";
import VariationsCard from "~/components/cards/VariationsCard.vue";
import RequiredSkillsCard from "~/components/cards/requiredSkills/RequiredSkillsCard.vue";
import MarketCard from "~/components/cards/market/MarketCard.vue";
import TypeManufacturing from "~/components/cards/TypeManufacturing.vue";
import InsuranceCard from "~/components/cards/InsuranceCard.vue";

const props = defineProps<{
	inventoryType: InventoryType,
	component: string | undefined,
	title: string,
	dogmaAttributes: DogmaAttribute[]
}>();
</script>

<template>
	<template v-if="component == 'defenses'"></template>
	<BasicCard v-else-if="component == 'basic'"
		:title="title"
		:inventory-type="inventoryType"
		:dogma-attributes="dogmaAttributes" />
	<ReprocessingCard v-else-if="component == 'reprocessing'"
		:title="title"
		:inventory-type="inventoryType"
		:dogma-attributes="dogmaAttributes" />
	<VariationsCard v-else-if="component == 'variations'"
		:title="title"
		:inventory-type="inventoryType"
		:dogma-attributes="dogmaAttributes" />
	<RequiredSkillsCard v-else-if="component == 'requiredSkills'"
		:title="title"
		:inventory-type="inventoryType"
		:dogma-attributes="dogmaAttributes" />
	<MarketCard v-else-if="component == 'market'"
		:title="title"
		:inventory-type="inventoryType"
		:dogma-attributes="dogmaAttributes" />
	<TypeManufacturing v-else-if="component == 'typeManufacturing'"
		:title="title"
		:inventory-type="inventoryType"
		:dogma-attributes="dogmaAttributes" />
	<InsuranceCard v-else-if="component == 'insurance'"
		:title="title"
		:inventory-type="inventoryType"
		:dogma-attributes="dogmaAttributes" />
	<DefaultCard v-else
		:title="title"
		:component="component"
		:inventory-type="inventoryType"
		:dogma-attributes="dogmaAttributes" />
</template>
