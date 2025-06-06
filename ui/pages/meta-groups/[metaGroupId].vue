<script setup lang="ts">
import refdataApi, {cacheMarketGroupBundle} from "~/refdata";
import {type MetaGroup} from "~/refdata-openapi";
import TypeLink from "~/components/helpers/TypeLink.vue";
import {getIntRouteParam} from "~/lib/routeUtils";
import {tr} from "~/lib/translate";
import MarketPrice from "~/components/helpers/MarketPrice.vue";
import CardsContainer from "~/components/cards/CardsContainer.vue";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import AttributeList from "~/components/attr/AttributeList.vue";
import AttributeListItem from "~/components/attr/AttributeListItem.vue";
import ExternalLink from "~/components/helpers/ExternalLink.vue";
import MetaGroupLink from "~/components/helpers/MetaGroupLink.vue";

const route = useRoute();
const {locale} = useI18n();

const metaGroupId: number = getIntRouteParam(route, "metaGroupId");
// await cacheMarketGroupBundle(metaGroupId);

const metaGroup: MetaGroup = await refdataApi.getMetaGroup({metaGroupId: metaGroupId});
useHead({
	title: tr(metaGroup.name, locale.value)
});

const typeIds = metaGroup.typeIds?.filter((typeId) => typeId !== undefined) || [];

</script>

<template>
	<div>
		<h1 v-if="metaGroup.name" class="mb-3">{{ tr(metaGroup.name, locale) }}</h1>

		<CardsContainer>
			<CardWrapper title="Meta group">
				<ul>
					<li>Meta Group ID: {{ metaGroup.metaGroupId }}</li>
					<li>Icon ID: {{ metaGroup.iconId }}</li>
					<li>Icon suffix: {{ metaGroup.iconSuffix }}</li>
					<li>Color: {{ metaGroup.color }}</li>
					<li>Types: {{ metaGroup.typeIds?.length || 0 }}</li>
					<li>
						<ExternalLink :url="`https://ref-data.everef.net/meta_groups/${metaGroup.metaGroupId}`">Reference Data JSON</ExternalLink>
					</li>
					<li>
						<ExternalLink :url="`https://sde.jita.space/latest/universe/metaGroups/${metaGroup.metaGroupId}`">SDE JSON</ExternalLink>
					</li>
				</ul>
			</CardWrapper>
		</CardsContainer>

		<client-only>
			<table class="standard-table">
				<thead>
					<tr>
						<th>Type</th>
						<th class="text-right">Price</th>
					</tr>
				</thead>
				<tbody>
					<tr v-for="typeId in typeIds" :key="typeId">
						<td>
							<TypeLink :type-id="typeId" />
						</td>
						<td class="text-right">
							<MarketPrice :type-id="typeId" order-type="sell" />
						</td>
					</tr>
				</tbody>
			</table>
		</client-only>
	</div>
</template>
