<script setup lang="ts">
import refdataApi, {cacheMarketGroupBundle} from "~/refdata";
import {type MarketGroup} from "~/refdata-openapi";
import TypeLink from "~/components/helpers/TypeLink.vue";
import MarketGroupLink from "~/components/helpers/MarketGroupLink.vue";
import MarketGroupBreadcrumbs from "~/components/helpers/MarketGroupBreadcrumbs.vue";
import {getIntRouteParam} from "~/lib/routeUtils";
import {tr} from "~/lib/translate";
import MarketPrice from "~/components/helpers/MarketPrice.vue";

const route = useRoute();
const {locale} = useI18n();

const marketGroupId: number = getIntRouteParam(route, "marketGroupId");
await cacheMarketGroupBundle(marketGroupId);

const marketGroup: MarketGroup = await refdataApi.getMarketGroup({marketGroupId});
useHead({
	title: tr(marketGroup.name, locale.value)
});

const childIds = marketGroup.childMarketGroupIds?.filter((id) => id !== undefined) || [];
const childGroups = await Promise.all(childIds.map(async (marketGroupId) => await refdataApi.getMarketGroup({marketGroupId})));
const sortedChildIds = computed(() => childGroups.sort((a, b) => {
	const an = tr(a.name, locale.value) || "";
	const bn = tr(b.name, locale.value) || "";
	return an.localeCompare(bn);
})
	.map((group) => group.marketGroupId));

const typeIds = marketGroup.typeIds?.filter((typeId) => typeId !== undefined) || [];
const types = await Promise.all(typeIds.map(async (typeId) => await refdataApi.getType({typeId})));
const sortedTypeIds = computed(() => types.sort((a, b) => {
	const an = tr(a.name, locale.value) || "";
	const bn = tr(b.name, locale.value) || "";
	return an.localeCompare(bn);
})
	.map((type) => type.typeId)
	.filter((typeId) => typeId !== undefined));

</script>

<template>
	<div>
		<h1 v-if="marketGroup.name">{{ tr(marketGroup.name, locale) }}</h1>
		<div v-if="marketGroup.parentGroupId" class="mb-3">
			Market group: <MarketGroupBreadcrumbs :market-group-id="marketGroup.parentGroupId" />
		</div>
		<div class="flex flex-col">
			<MarketGroupLink class="py-2" v-for="childId in sortedChildIds"
				:key="childId"
				:marketGroupId="childId" />
		</div>
		<div class="flex flex-col">
			<div class="py-2" v-for="typeId in sortedTypeIds" :key="typeId">
				<TypeLink :typeId="typeId" /> <MarketPrice :type-id="typeId" order-type="sell" />
			</div>
		</div>
	</div>
</template>
