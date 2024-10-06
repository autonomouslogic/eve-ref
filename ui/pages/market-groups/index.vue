<script setup lang="ts">
import refdataApi, {cacheRootMarketGroupBundle} from "~/refdata";
import MarketGroupLink from "~/components/helpers/MarketGroupLink.vue";
import {tr} from "~/lib/translate";
const {locale} = useI18n();

await cacheRootMarketGroupBundle();
useHead({
	title: "Market Groups"
});

const marketGroupIds: number[] = await refdataApi.getRootMarketGroups();

const groups = await Promise.all(marketGroupIds.map(async (marketGroupId) => await refdataApi.getMarketGroup({marketGroupId})));
const sortedGroupIds = computed(() => groups.sort((a, b) => {
	const an = tr(a.name, locale.value) || "";
	const bn = tr(b.name, locale.value) || "";
	return an.localeCompare(bn);
})
	.map((group) => group.marketGroupId)
	.filter((groupId) => groupId !== undefined));
</script>

<template>
	<div>
		<h1>Market Groups</h1>
		<div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
			<MarketGroupLink
				class="py-2"
				v-for="marketGroupId in sortedGroupIds"
				:key="marketGroupId"
				:marketGroupId="marketGroupId">
			</MarketGroupLink>
		</div>
	</div>
</template>
