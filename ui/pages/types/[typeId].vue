<script setup lang="ts">
import refdataApi from "~/refdata";
import {InventoryGroup, InventoryType} from "~/refdata-openapi";
import TraitsContainer from "~/components/types/traits/TraitsContainer.vue";
import CategoryLink from "~/components/helpers/CategoryLink.vue";
import GroupLink from "~/components/helpers/GroupLink.vue";
import MarketGroupBreadcrumbs from "~/components/helpers/MarketGroupBreadcrumbs.vue";
import FormattedCurrency from "~/components/helpers/FormattedCurrency.vue";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import TraitsCard from "~/components/cards/TraitsCard.vue";
import BasicsCard from "~/components/cards/BasicsCard.vue";
import DogmaCard from "~/components/cards/DogmaCard.vue";

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

	<CardsContainer>
		<BasicsCard :inventory-type="inventoryType" />
		<TraitsCard :inventory-type="inventoryType" />
		<DogmaCard :inventory-type="inventoryType" />
	</CardsContainer>

	<h2>Description</h2>
	<p>{{ inventoryType.description[locale] }}</p>
</template>
