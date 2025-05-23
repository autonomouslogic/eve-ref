<script setup lang="ts">
import refdataApi, {cacheTypeBundle} from "~/refdata";
import {type InventoryGroup} from "~/refdata-openapi";
import CategoryLink from "~/components/helpers/CategoryLink.vue";
import GroupLink from "~/components/helpers/GroupLink.vue";
import MarketGroupBreadcrumbs from "~/components/helpers/MarketGroupBreadcrumbs.vue";
import TypeCards from "~/components/types/TypeCards.vue";
import LinkParser from "~/components/helpers/LinkParser.vue";
import {getIntRouteParam} from "~/lib/routeUtils";
import {tr} from "~/lib/translate";
import EveImage from "~/components/icons/EveImage.vue";
import {getTypeIconUrl} from "~/lib/urls";
import {SHIP} from "~/lib/categoryConstants";
import ExternalLink from "~/components/helpers/ExternalLink.vue";

const {locale} = useI18n();
const route = useRoute();
const typeId: number = getIntRouteParam(route, "typeId");
if (!typeId) {
	console.error("typeId is null");
}

await cacheTypeBundle(typeId);

const inventoryType = await refdataApi.getType({typeId});
if (inventoryType == undefined) {
	throw new Error(`Inventory type ${typeId} not found`);
}
if (typeof inventoryType.groupId !== "number") {
	throw new Error(`Inventory type ${typeId} has no group ID`);
}

const pageTitle = tr(inventoryType.name, locale.value);
let pageDescription = tr(inventoryType.description, locale.value) || "";
if (pageDescription.length > 200) {
	pageDescription = pageDescription.substring(0, 200);
}
const typeIconUrl = inventoryType && inventoryType.typeId ? await getTypeIconUrl(inventoryType.typeId) : "";
const renderUrl = inventoryType && inventoryType.typeId ? await getTypeIconUrl(inventoryType.typeId, "render") : "";

useHead({
	title: pageTitle
});
useSeoMeta({
	ogDescription: pageDescription,
	ogImage: typeIconUrl
});

const inventoryGroup: InventoryGroup = await refdataApi.getGroup({groupId: inventoryType.groupId});
</script>

<template>
	<h1 v-if="inventoryType.name">
		<EveImage :type-id="inventoryType.typeId || 0" />
		{{ tr(inventoryType.name, locale) }}
	</h1>
	<div class="mb-3">
		<div v-if="inventoryType.marketGroupId">
			Market group: <MarketGroupBreadcrumbs :market-group-id="inventoryType.marketGroupId" />
		</div>
		<div>
			Inventory group: <CategoryLink :categoryId="inventoryGroup.categoryId"></CategoryLink> &gt;
			<GroupLink :groupId="inventoryType.groupId"></GroupLink>
		</div>
	</div>
	<div v-if="inventoryType.categoryId == SHIP" class="mb-3">
		<ExternalLink :url="`${renderUrl}?size=1024`">View ship render</ExternalLink>
	</div>

	<TypeCards :inventory-type="inventoryType" />

	<h2>Description</h2>
	<p v-if="inventoryType.description">
		<LinkParser :content="tr(inventoryType.description, locale) || ''"/>
	</p>
</template>
