<script setup lang="ts">
import refdataApi from "~/refdata";
import {type MarketGroup} from "~/refdata-openapi";
import TypeLink from "~/components/helpers/TypeLink.vue";
import MarketGroupLink from "~/components/helpers/MarketGroupLink.vue";
import MarketGroupBreadcrumbs from "~/components/helpers/MarketGroupBreadcrumbs.vue";
import {getIntRouteParam} from "~/lib/routeUtils";
import {prepMessages} from "~/lib/translate";

const route = useRoute();
const {locale} = useI18n();

const marketGroupId: number = getIntRouteParam(route, "marketGroupId");
const marketGroup: MarketGroup = await refdataApi.getMarketGroup({marketGroupId});
useHead({
	title: prepMessages(marketGroup.name)[locale.value]
});
</script>

<template>
	<div>
		<h1 v-if="marketGroup.name">{{ prepMessages(marketGroup.name)[locale] }}</h1>
		<div v-if="marketGroup.parentGroupId" class="mb-3">
			Market group: <MarketGroupBreadcrumbs :market-group-id="marketGroup.parentGroupId" />
		</div>
		<div class="flex flex-col">
			<MarketGroupLink class="py-2" v-for="marketGroupId in marketGroup.childMarketGroupIds" :key="marketGroupId" :marketGroupId="marketGroupId"></MarketGroupLink>
		</div>
		<div class="flex flex-col">
			<TypeLink
				class="py-2"
				v-for="typeId in marketGroup.typeIds"
				:key="typeId"
				:typeId="typeId">
			</TypeLink>
		</div>
	</div>
</template>
