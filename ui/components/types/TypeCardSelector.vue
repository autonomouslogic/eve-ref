<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import DefaultCard from "~/components/cards/DefaultCard.vue";
import BasicCard from "~/components/cards/BasicCard.vue";
import ReprocessingCard from "~/components/cards/ReprocessingCard.vue";
import VariationsCard from "~/components/cards/VariationsCard.vue";
import RequiredSkillsCard from "~/components/cards/requiredSkills/RequiredSkillsCard.vue";
import TypeManufacturing from "~/components/cards/TypeManufacturing.vue";
import BlueprintManufacturing from "~/components/cards/BlueprintManufacturing.vue";
import BlueprintCopyingCard from "~/components/cards/BlueprintCopyingCard.vue";
import BlueprintResearchCard from "~/components/cards/BlueprintResearchCard.vue";
import BlueprintInventionCard from "~/components/cards/BlueprintInventionCard.vue";
import TypeInfoCard from "~/components/cards/TypeInfoCard.vue";
import BoosterCard from "~/components/cards/BoosterCard.vue";
import IndustryCard from "~/components/cards/IndustryCard.vue";
import PlanetaryInteractionCard from "~/components/cards/PlanetaryInteractionCard.vue";

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
	<CardsMarketCard v-else-if="component == 'market'"
		:title="title"
		:inventory-type="inventoryType"
		:dogma-attributes="dogmaAttributes" />
	<TypeManufacturing v-else-if="component == 'typeManufacturing'"
		:title="title"
		:inventory-type="inventoryType"
		:dogma-attributes="dogmaAttributes" />
	<BlueprintManufacturing v-else-if="component == 'blueprintManufacturing'"
		:title="title"
		:inventory-type="inventoryType"
		:dogma-attributes="dogmaAttributes" />
	<BlueprintResearchCard v-else-if="component == 'blueprintResearch'"
		:title="title"
		:inventory-type="inventoryType"
		:dogma-attributes="dogmaAttributes" />
	<BlueprintCopyingCard v-else-if="component == 'blueprintCopying'"
		:title="title"
		:inventory-type="inventoryType"
		:dogma-attributes="dogmaAttributes" />
	<BlueprintInventionCard v-else-if="component == 'blueprintInvention'"
		:title="title"
		:inventory-type="inventoryType"
		:dogma-attributes="dogmaAttributes" />
	<CardsInsuranceCard v-else-if="component == 'insurance'"
		:title="title"
		:inventory-type="inventoryType"
		:dogma-attributes="dogmaAttributes" />
	<TypeInfoCard v-else-if="component == 'typeInfo'"
		:title="title"
		:inventory-type="inventoryType"
		:dogma-attributes="dogmaAttributes" />
	<BoosterCard v-else-if="component == 'booster'"
		:title="title"
		:inventory-type="inventoryType"
		:dogma-attributes="dogmaAttributes" />
	<IndustryCard v-else-if="component == 'industry'"
		:title="title"
		:inventory-type="inventoryType"
		:dogma-attributes="dogmaAttributes" />
	<PlanetaryInteractionCard v-else-if="component == 'planetInteraction'"
		:title="title"
		:inventory-type="inventoryType"
		:dogma-attributes="dogmaAttributes" />
	<DefaultCard v-else
		:title="title"
		:component="component"
		:inventory-type="inventoryType"
		:dogma-attributes="dogmaAttributes" />
</template>
