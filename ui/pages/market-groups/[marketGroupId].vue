<script setup lang="ts">
import refdataApi, {cacheMarketGroupBundle} from "~/refdata";
import {type MarketGroup} from "~/refdata-openapi";
import TypeLink from "~/components/helpers/TypeLink.vue";
import MarketGroupLink from "~/components/helpers/MarketGroupLink.vue";
import MarketGroupBreadcrumbs from "~/components/helpers/MarketGroupBreadcrumbs.vue";
import {getIntRouteParam} from "~/lib/routeUtils";
import {tr} from "~/lib/translate";
import MarketPrice from "~/components/helpers/MarketPrice.vue";
import CategoryLink from "~/components/helpers/CategoryLink.vue";
import GroupLink from "~/components/helpers/GroupLink.vue";
import {getCategoryDogma, getGroupDogma, getMainDogma} from "~/lib/mainDogmaAttributes";
import CardWrapper from "~/components/cards/CardWrapper.vue";

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
const sortedChildGroups = computed(() => childGroups.sort((a, b) => {
	const an = tr(a.name, locale.value) || "";
	const bn = tr(b.name, locale.value) || "";
	return an.localeCompare(bn);
}));

const typeIds = marketGroup.typeIds?.filter((typeId) => typeId !== undefined) || [];
const types = await Promise.all(typeIds.map(async (typeId) => await refdataApi.getType({typeId})));
const sortedTypeIds = computed(() => types.sort((a, b) => {
	const an = tr(a.name, locale.value) || "";
	const bn = tr(b.name, locale.value) || "";
	return an.localeCompare(bn);
})
	.map((type) => type.typeId)
	.filter((typeId) => typeId !== undefined));

const categoryIds = [...new Set(types.map(type => type.categoryId).filter((categoryId) => categoryId !== undefined))];
const groupIds = [...new Set(types.map(type => type.groupId).filter((categoryId) => categoryId !== undefined))];

const comparisonDogmaAttributes: string[] = groupIds.length == 1 ? (await getGroupDogma(groupIds[0]) || [])
	: categoryIds.length == 1 ? (await getCategoryDogma(categoryIds[0]) || [])
		: [];
</script>

<template>
	<div>
		<h1 v-if="marketGroup.name">{{ tr(marketGroup.name, locale) }}</h1>

		<div v-if="marketGroup.parentGroupId" class="mb-3">
			Market group: <MarketGroupBreadcrumbs :market-group-id="marketGroup.marketGroupId" />
		</div>

		<div v-if="categoryIds.length == 1" class="mb-3">
			Shared inventory group: <CategoryLink :categoryId="categoryIds[0]"></CategoryLink><template v-if="groupIds.length == 1"> &gt;
				<GroupLink :groupId="groupIds[0]"></GroupLink>
			</template>
		</div>

		<table class="standard-table" v-if="sortedChildGroups && sortedChildGroups.length > 1">
			<thead>
				<tr>
					<th>Market group</th>
					<th class="text-right">Child groups</th>
					<th class="text-right">Types</th>
				</tr>
			</thead>
			<tbody>
				<tr v-for="group in sortedChildGroups" :key="group.marketGroupId">
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

		<CompareTable
			v-if="sortedTypeIds && sortedTypeIds.length > 0"
			:type-ids="sortedTypeIds"
			:dogma-attribute-names="comparisonDogmaAttributes"
			direction="vertical"
			:compact-attribute-names="true"
			:show-meta-group="true"
			:load-bundles="false" />
	</div>
</template>
