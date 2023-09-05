<script setup lang="ts">
import refdataApi from "~/refdata";
import {MarketGroup} from "~/refdata-openapi";
import MarketGroupLink from "~/components/helpers/MarketGroupLink.vue";

const props = defineProps<{
	marketGroupId: number | undefined
}>();

const marketGroupIds: number[] = [];
let currentGroupId = ref(props.marketGroupId);

if (currentGroupId.value === undefined) {
	throw new Error("marketGroupId is required");
}

let marketGroup: MarketGroup = await refdataApi.getMarketGroup({marketGroupId: currentGroupId.value});
marketGroupIds.unshift(currentGroupId.value);
while (marketGroup && marketGroup.parentGroupId) {
	currentGroupId.value = marketGroup.parentGroupId;
	marketGroup = await refdataApi.getMarketGroup({marketGroupId: currentGroupId.value});
	marketGroupIds.unshift(currentGroupId.value);
}
</script>

<template>
	<template v-for="(marketGroupId, i) in marketGroupIds" :key="i">
		<MarketGroupLink :market-group-id="marketGroupId"></MarketGroupLink><template v-if="i < marketGroupIds.length - 1"> &gt; </template>
	</template>
</template>
