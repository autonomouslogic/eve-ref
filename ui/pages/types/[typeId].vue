<script setup lang="ts">
import refdataApi from "~/refdata";
import {DogmaTypeAttribute, InventoryGroup, InventoryType} from "~/refdata-openapi";
import CategoryLink from "~/components/helpers/CategoryLink.vue";
import GroupLink from "~/components/helpers/GroupLink.vue";
import MarketGroupBreadcrumbs from "~/components/helpers/MarketGroupBreadcrumbs.vue";
import TraitsCard from "~/components/cards/TraitsCard.vue";
import BasicsCard from "~/components/cards/BasicsCard.vue";
import DogmaCard from "~/components/cards/DogmaCard.vue";
import typeCards from "~/conf/typeCards";

const {locale} = useI18n();
const route = useRoute();
const typeId = route.params.typeId;

if (!typeId) {
	console.error("typeId is null");
}

const inventoryType: InventoryType = await refdataApi.getType({typeId});
const inventoryGroup: InventoryGroup = await refdataApi.getGroup({groupId: inventoryType.groupId});


// Load all dogma attribute names for this type.
const attributeNames = {};
if (inventoryType.dogmaAttributes) {
	var promises = [];
	for (const attrId in inventoryType.dogmaAttributes) {
		promises.push((async () => {
			var attr = await refdataApi.getDogmaAttribute({attributeId: parseInt(attrId)});
			attributeNames[attr.name] = attrId;
		})());
	}
	await Promise.all(promises);
}



</script>

<template>
	<code>{{JSON.stringify(attributeNames, null, 2)}}</code>
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
