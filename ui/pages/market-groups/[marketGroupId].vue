<script setup lang="ts">
import refdataApi from "~/refdata";
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
const marketGroup: MarketGroup = await refdataApi.getMarketGroup({marketGroupId});
useHead({
	title: tr(marketGroup.name, locale.value)
});
</script>

<template>
	<div>
		<h1 v-if="marketGroup.name">{{ tr(marketGroup.name, locale) }}</h1>
		<div v-if="marketGroup.parentGroupId" class="mb-3">
			Market group: <MarketGroupBreadcrumbs :market-group-id="marketGroup.parentGroupId" />
		</div>
		<div class="flex flex-col">
			<MarketGroupLink class="py-2" v-for="marketGroupId in marketGroup.childMarketGroupIds" :key="marketGroupId" :marketGroupId="marketGroupId"></MarketGroupLink>
		</div>
		<div class="flex flex-col">
			<div class="py-2" v-for="typeId in marketGroup.typeIds" :key="typeId">
				<TypeLink :typeId="typeId" /> <MarketPrice :type-id="typeId" order-type="sell" />
			</div>
		</div>
	</div>
</template>
