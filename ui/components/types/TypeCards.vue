<script setup lang="ts">
import refdataApi from "~/refdata";
import {InventoryType} from "~/refdata-openapi";
import TraitsCard from "~/components/cards/TraitsCard.vue";
import typeCardsConfig from "~/conf/typeCardsConfig";
import DefaultCard from "~/components/cards/DefaultCard.vue";
import BasicCard from "~/components/cards/BasicCard.vue";

const props = defineProps<{
	inventoryType: InventoryType
}>();

// Load all dogma attribute names for this type.
const dogmaAttributes = {};
if (props.inventoryType.dogmaAttributes) {
	var promises = [];
	for (const attrId in props.inventoryType.dogmaAttributes) {
		promises.push((async () => {
			var attr = await refdataApi.getDogmaAttribute({attributeId: parseInt(attrId)});
			dogmaAttributes[attr.name] = attr;
		})());
	}
	await Promise.all(promises);
}

// For each card, extract the attributes needed for each card.
const cardAttributes = {};
for (const cardName in typeCardsConfig) {
	const cardConfig = typeCardsConfig[cardName];
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
</script>

<template>
	<TraitsCard :inventory-type="inventoryType" />
	<CardsContainer>
		<template v-for="(attributes, cardId) in cardAttributes" :key="cardId">
			<BasicCard v-if="typeCardsConfig[cardId].component == 'basic'"
				:title="typeCardsConfig[cardId].name[locale]"
				:inventory-type="inventoryType"
				:dogma-attributes="attributes" />
			<DefaultCard v-else
				:title="typeCardsConfig[cardId].name[locale]"
				:inventory-type="inventoryType"
				:dogma-attributes="attributes" />
		</template>
		<!--		<BasicsCard :inventory-type="inventoryType" :dogma-attributes="cardAttributes['basics']" />-->
		<!--		<DogmaCard :inventory-type="inventoryType" />-->
	</CardsContainer>
</template>
