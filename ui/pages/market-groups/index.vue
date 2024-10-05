<script setup lang="ts">
import refdataApi, {cacheRootMarketGroupBundle} from "~/refdata";
import MarketGroupLink from "~/components/helpers/MarketGroupLink.vue";

await cacheRootMarketGroupBundle();
const marketGroupIds: number[] = await refdataApi.getRootMarketGroups();
useHead({
	title: "Market Groups"
});
</script>

<template>
	<div>
		<h1>Market Groups</h1>
		<div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
			<MarketGroupLink
				class="py-2"
				v-for="marketGroupId in marketGroupIds"
				:key="marketGroupId"
				:marketGroupId="marketGroupId">
			</MarketGroupLink>
		</div>
	</div>
</template>
