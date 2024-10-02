<script setup lang="ts">
import {type DogmaAttribute, type DogmaTypeAttribute, type InventoryType} from "~/refdata-openapi";
import TraitsCard from "~/components/cards/TraitsCard.vue";
import typeCardsConfig from "~/conf/typeCardsConfig";
import DefensesCard from "~/components/cards/defenses/DefensesCard.vue";
import TypeCardSelector from "~/components/types/TypeCardSelector.vue";
import {loadDogmaAttributesForType} from "~/lib/dogmaUtils";
import {prepMessages} from "~/lib/translate";
import VariationsCard from "~/components/cards/VariationsCard.vue";
import RequiredSkillsCard from "~/components/cards/requiredSkills/RequiredSkillsCard.vue";
import DefaultCard from "~/components/cards/DefaultCard.vue";

const {locale} = useI18n();

const props = defineProps<{
	inventoryType: InventoryType
}>();

// Load all dogma attribute names for this type.
const dogmaAttributes = await loadDogmaAttributesForType(props.inventoryType);
const allDogmaAttributes: { [key: string]: DogmaAttribute } = JSON.parse(JSON.stringify(dogmaAttributes));

// For each card, extract the attributes needed for each card.
const cardAttributes: {[key: string]: DogmaTypeAttribute[] } = {};
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
		else if (allDogmaAttributes[attrName]) {
			cardAttributes[cardName].push(allDogmaAttributes[attrName]);
		}
	}
}

// Map the remaining attributes into the "other" card.
if (Object.keys(dogmaAttributes).length > 0) {
	cardAttributes.other = Object.values(dogmaAttributes);
}

// Full-width cards.
const fullWidthCards = [
	"traits",
	"defenses",
	"variations"
];

const contentCards: {[key: string]: DogmaTypeAttribute[] } = {};
for (const cardName in cardAttributes) {
	if (fullWidthCards.includes(cardName)) {
		continue;
	}
	contentCards[cardName] = cardAttributes[cardName];
}
</script>

<template>
	<TraitsCard class="my-4" :inventory-type="inventoryType" :full-width="true"/>
	<DefensesCard class="my-4" :title="prepMessages(typeCardsConfig.defenses.name)[locale]"
		:inventory-type="inventoryType"
		:dogma-attributes="cardAttributes.defenses"
		:full-width="true"/>
	<VariationsCard class="my-4" :title="prepMessages(typeCardsConfig.variations.name)[locale]"
		:inventory-type="inventoryType"
		:dogma-attributes="cardAttributes.variations"
		:full-width="true"/>
	<CardsContainer>
		<template v-for="(attributes, cardId) in contentCards" :key="cardId">
			<TypeCardSelector
				:component="typeCardsConfig[cardId].component"
				:title="prepMessages(typeCardsConfig[cardId].name)[locale]"
				:inventory-type="inventoryType"
				:dogma-attributes="attributes"
			/>
		</template>
	</CardsContainer>
</template>
