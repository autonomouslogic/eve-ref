<script setup lang="ts">
import refdataApi from "~/refdata";
import {InventoryGroup, InventoryType} from "~/refdata-openapi";
import TraitsContainer from "~/components/types/traits/TraitsContainer.vue";
import CategoryLink from "~/components/helpers/CategoryLink.vue";
import GroupLink from "~/components/helpers/GroupLink.vue";
import MarketGroupBreadcrumbs from "~/components/helpers/MarketGroupBreadcrumbs.vue";
import FormattedCurrency from "~/components/helpers/FormattedCurrency.vue";

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
	<div>
		<h1>{{ inventoryType.name[locale] }}</h1>
		<p>
			<CategoryLink :categoryId="inventoryGroup.categoryId"></CategoryLink> &gt;
			<GroupLink :groupId="inventoryType.groupId"></GroupLink>
		</p>
		<p v-if="inventoryType.marketGroupId">
			<MarketGroupBreadcrumbs :market-group-id="inventoryType.marketGroupId"></MarketGroupBreadcrumbs>
		</p>
		<img :src="`https://images.evetech.net/types/${inventoryType.typeId}/icon`" alt="">

		<p>Type ID: {{ route.params.typeId }}</p>
		<p>Description: {{ inventoryType.description[locale] }}</p>
		<p>Price:
			<FormattedCurrency :price="inventoryType.basePrice"></FormattedCurrency>
		</p>
		<TraitsContainer :traits="inventoryType.traits"></TraitsContainer>

		<h2>Dogma values</h2>
		<ul>
			<li v-for="(attributeValue, i) in inventoryType.dogmaAttributes" :key="i">
				<DogmaAttributeValue :value="attributeValue.value" :attribute-id="attributeValue.attributeId" />
			</li>
		</ul>
	</div>
</template>
