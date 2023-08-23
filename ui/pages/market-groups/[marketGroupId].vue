<script setup lang="ts">
import refdataApi from "~/refdata";
import {MarketGroup} from "~/refdata-openapi";
import TypeLink from "~/components/helpers/TypeLink.vue";
import MarketGroupLink from "~/components/helpers/MarketGroupLink.vue";
import MarketGroupBreadcrumbs from "~/components/helpers/MarketGroupBreadcrumbs.vue";

const route = useRoute();
const {locale} = useI18n();

const marketGroupId: number = parseInt(route.params.marketGroupId[0] ?? route.params.marketGroupId);
const marketGroup: MarketGroup = await refdataApi.getMarketGroup({marketGroupId});
</script>

<template>
	<div>
		<h1 v-if="marketGroup.name">{{ marketGroup.name[locale] }}</h1>
		<p v-if="marketGroup.parentGroupId">
			<MarketGroupBreadcrumbs :market-group-id="marketGroup.parentGroupId"></MarketGroupBreadcrumbs>
		</p>
		<ul>
			<li v-for="marketGroupId in marketGroup.childMarketGroupIds" :key="marketGroupId">
				<MarketGroupLink :marketGroupId="marketGroupId"></MarketGroupLink>
			</li>
		</ul>
		<ul>
			<li v-for="typeId in marketGroup.typeIds" :key="typeId">
				<TypeLink :typeId="typeId"></TypeLink>
			</li>
		</ul>
	</div>
</template>
