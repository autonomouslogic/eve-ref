<script setup lang="ts">
import refdataApi from "~/refdata";
import MarketGroupName from "~/components/helpers/MarketGroupName.vue";
import {STRUCTURE_COMPARISON_REFINERY_DOGMA_NAMES} from "~/lib/structureConstants";
import {prepMessages} from "~/lib/translate";

const {locale} = useI18n();

const marketGroupId = 2327;
const marketGroup = await refdataApi.getMarketGroup({marketGroupId});
useHead({
	title: prepMessages(marketGroup.name)[locale.value]
});
const structureIds = marketGroup.typeIds;
if (!structureIds) {
	throw new Error(`Market group ${marketGroupId} has no type IDs`);
}
structureIds.sort();


const attrNames = STRUCTURE_COMPARISON_REFINERY_DOGMA_NAMES;
</script>

<template>
	<h1>
		<MarketGroupName :market-group-id="marketGroupId" />
	</h1>
	<CompareTable :type-ids="structureIds" :dogma-attribute-names="attrNames" />
</template>
