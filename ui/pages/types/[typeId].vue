<script setup lang="ts">
import refdataApi from "~/refdata";
import {InventoryGroup, InventoryType} from "~/refdata-openapi";
import CategoryLink from "~/components/helpers/CategoryLink.vue";
import GroupLink from "~/components/helpers/GroupLink.vue";
import MarketGroupBreadcrumbs from "~/components/helpers/MarketGroupBreadcrumbs.vue";
import TraitsCard from "~/components/cards/TraitsCard.vue";
import typeCards from "~/conf/typeCards";
import DefaultCard from "~/components/cards/DefaultCard.vue";
import BasicCard from "~/components/cards/BasicCard.vue";

const {locale} = useI18n();
const route = useRoute();
const typeId = route.params.typeId;

if (!typeId) {
	console.error("typeId is null");
}

const inventoryType: InventoryType = await refdataApi.getType({typeId});
const inventoryGroup: InventoryGroup = await refdataApi.getGroup({groupId: inventoryType.groupId});

// Load all dogma attribute names for this type.
const dogmaAttributes = {};
if (inventoryType.dogmaAttributes) {
	var promises = [];
	for (const attrId in inventoryType.dogmaAttributes) {
		promises.push((async () => {
			var attr = await refdataApi.getDogmaAttribute({attributeId: parseInt(attrId)});
			dogmaAttributes[attr.name] = attr;
		})());
	}
	await Promise.all(promises);
}

// For each card, extract the attributes needed for each card.
const cardAttributes = {};
for (const cardName in typeCards) {
	const cardConfig = typeCards[cardName];
	cardAttributes[cardName] = [];
	if (cardConfig.dogmaAttributes === undefined) {
		continue;
	}
	for (const attrName of cardConfig.dogmaAttributes) {
		if (dogmaAttributes[attrName]) {
			cardAttributes[cardName].push(dogmaAttributes[attrName]);
			delete dogmaAttributes[attrName];
		}
	}
}

// Map the remaining attributes into the "other" card.
if (Object.keys(dogmaAttributes).length > 0) {
	cardAttributes.other = Object.values(dogmaAttributes);
}
console.log(cardAttributes);
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

	<TraitsCard :inventory-type="inventoryType" />
	<CardsContainer>
		<template v-for="(attributes, cardId) in cardAttributes" :key="cardId">
			<BasicCard v-if="typeCards[cardId].component == 'basic'"
				:title="typeCards[cardId].name[locale]"
				:inventory-type="inventoryType"
				:dogma-attributes="attributes" />
			<DefaultCard v-else
				:title="typeCards[cardId].name[locale]"
				:inventory-type="inventoryType"
				:dogma-attributes="attributes" />
		</template>
		<!--		<BasicsCard :inventory-type="inventoryType" :dogma-attributes="cardAttributes['basics']" />-->
		<!--		<DogmaCard :inventory-type="inventoryType" />-->
	</CardsContainer>

	<h2>Description</h2>
	<p>{{ inventoryType.description[locale] }}</p>
</template>
