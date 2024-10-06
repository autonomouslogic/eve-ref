<script setup lang="ts">
import refdataApi from "~/refdata";
import MarketGroupName from "~/components/helpers/MarketGroupName.vue";
import {STRUCTURE_COMPARISON_CITADELS_DOGMA_NAMES} from "~/lib/structureConstants";
import {tr} from "~/lib/translate";

const {locale} = useI18n();

const marketGroupId = 2200;
const marketGroup = await refdataApi.getMarketGroup({marketGroupId});
useHead({
	title: tr(marketGroup.name, locale.value)
});
useSeoMeta({
	ogDescription: "Faction citadels overview."
});
const structureIds = marketGroup.typeIds;
if (!structureIds) {
	throw new Error(`Market group ${marketGroupId} has no type IDs`);
}
structureIds.sort();


const attrNames = STRUCTURE_COMPARISON_CITADELS_DOGMA_NAMES;
</script>

<template>
	<h1>
		<MarketGroupName :market-group-id="marketGroupId" />
	</h1>
	<CompareTable :type-ids="structureIds" :dogma-attribute-names="attrNames" />
</template>
