<script setup lang="ts">
import refdataApi from "~/refdata";
import {MarketGroup} from "~/refdata-openapi";
import MarketGroupLink from "~/components/helpers/MarketGroupLink.vue";

const props = defineProps<{
	marketGroupId: number | undefined
}>();

const marketGroupIds: number[] = [];
let currentGroupId = props.marketGroupId;

if (currentGroupId === undefined) {
  throw new Error("marketGroupId is required");
}

let marketGroup: MarketGroup = await refdataApi.getMarketGroup({marketGroupId: currentGroupId});
marketGroupIds.unshift(currentGroupId);
while (marketGroup && marketGroup.parentGroupId) {
	currentGroupId = marketGroup.parentGroupId;
	marketGroup = await refdataApi.getMarketGroup({marketGroupId: currentGroupId});
	marketGroupIds.unshift(currentGroupId);
}
</script>

<template>
	<template v-for="(marketGroupId, i) in marketGroupIds" :key="i">
		<MarketGroupLink :market-group-id="marketGroupId"></MarketGroupLink><template v-if="i < marketGroupIds.length - 1"> &gt; </template>
	</template>
</template>
