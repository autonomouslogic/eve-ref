<script setup lang="ts">
import refdataApi from "~/refdata";
import {InventoryGroup, InventoryType} from "~/refdata-openapi";
import CategoryLink from "~/components/helpers/CategoryLink.vue";
import GroupLink from "~/components/helpers/GroupLink.vue";
import MarketGroupBreadcrumbs from "~/components/helpers/MarketGroupBreadcrumbs.vue";
import TypeCards from "~/components/types/TypeCards.vue";
import LinkParser from "~/components/helpers/LinkParser.vue";

const {locale} = useI18n();
const route = useRoute();
const typeId = route.params.typeId;

if (!typeId) {
	console.error("typeId is null");
}

const inventoryType: InventoryType = await refdataApi.getType({typeId});
const inventoryGroup: InventoryGroup = await refdataApi.getGroup({groupId: inventoryType.groupId});

</script>

<template>
	<h1>{{ inventoryType.name[locale] }}</h1>
	<p>
		<CategoryLink :categoryId="inventoryGroup.categoryId"></CategoryLink> &gt;
		<GroupLink :groupId="inventoryType.groupId"></GroupLink>
	</p>
	<p v-if="inventoryType.marketGroupId">
		<MarketGroupBreadcrumbs :market-group-id="inventoryType.marketGroupId"></MarketGroupBreadcrumbs>
	</p>
	<img :src="`https://images.evetech.net/types/${inventoryType.typeId}/icon`" alt="">

	<TypeCards :inventory-type="inventoryType" />

	<h2>Description</h2>
	<p><LinkParser :content="inventoryType.description[locale]"/></p>
</template>
