<script setup lang="ts">
import refdataApi from "~/refdata";
import MarketGroupLink from "~/components/helpers/MarketGroupLink.vue";

const marketGroupIds: number[] = await refdataApi.getRootMarketGroups();
useHead({
	title: "Market Groups"
});
</script>

<template>
	<div>
		<h1>Market Groups</h1>
		<ul>
			<li v-for="marketGroupId in marketGroupIds" :key="marketGroupId">
				<MarketGroupLink :marketGroupId="marketGroupId"></MarketGroupLink>
			</li>
		</ul>
	</div>
</template>
