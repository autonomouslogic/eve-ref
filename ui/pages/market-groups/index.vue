<script setup lang="ts">
import refdataApi, {cacheRootMarketGroupBundle} from "~/refdata";
import MarketGroupLink from "~/components/helpers/MarketGroupLink.vue";
import {tr} from "~/lib/translate";
import MarketPrice from "~/components/helpers/MarketPrice.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import CardsContainer from "~/components/cards/CardsContainer.vue";
import ExternalLink from "~/components/helpers/ExternalLink.vue";

const {locale} = useI18n();

await cacheRootMarketGroupBundle();
useHead({
	title: "Market Groups"
});

const marketGroupIds: number[] = await refdataApi.getRootMarketGroups();

const groups = await Promise.all(marketGroupIds.map(async (marketGroupId) => await refdataApi.getMarketGroup({marketGroupId})));
const sortedGroups = computed(() => groups.sort((a, b) => {
	const an = tr(a.name, locale.value) || "";
	const bn = tr(b.name, locale.value) || "";
	return an.localeCompare(bn);
}));
</script>

<template>
	<div>
		<h1 class="mb-3">Market Groups</h1>

		<table class="standard-table">
			<thead>
				<tr>
					<th>Market group</th>
					<th class="text-right">Child groups</th>
					<th class="text-right">Types</th>
				</tr>
			</thead>
			<tbody>
				<tr v-for="group in sortedGroups" :key="group.marketGroupId">
					<td>
						<MarketGroupLink :marketGroupId="group.marketGroupId" />
					</td>
					<td class="text-right">
						{{ group.childMarketGroupIds?.length || 0 }}
					</td>
					<td class="text-right">
						{{ group.typeIds?.length || 0 }}
					</td>
				</tr>
			</tbody>
		</table>
	</div>
</template>
