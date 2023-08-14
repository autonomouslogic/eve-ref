<script setup lang="ts">
import {DogmaAttribute, InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import FormattedCurrency from "~/components/helpers/FormattedCurrency.vue";
import DogmaListItems from "~/components/cards/DogmaListItems.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import CubicMeters from "~/components/dogma/units/CubicMeters.vue";
import Kilogram from "~/components/dogma/units/Kilogram.vue";
import MarketGroupLink from "~/components/helpers/MarketGroupLink.vue";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();
</script>

<template>
	<CardWrapper title="Basics">
		<ul>
			<li v-if="inventoryType.typeId">Type ID: {{ inventoryType.typeId }}</li>
			<li v-if="inventoryType.basePrice">Base price: <FormattedCurrency :price="inventoryType.basePrice" /></li>
			<li v-if="inventoryType.capacity">Capacity: <CubicMeters :value="inventoryType.capacity" /></li>
			<li v-if="inventoryType.factionId">Faction ID: {{ inventoryType.factionId }}</li>
			<li v-if="inventoryType.graphicId">Graphic ID: {{ inventoryType.graphicId }}</li>
			<li v-if="inventoryType.groupId">Group ID: {{ inventoryType.groupId }}</li>
			<li v-if="inventoryType.iconId">Icon ID: {{ inventoryType.iconId }}</li>
			<li v-if="inventoryType.marketGroupId">Market group ID: <MarketGroupLink :market-group-id="inventoryType.marketGroupId"/> [{{ inventoryType.marketGroupId }}]</li>
			<li v-if="inventoryType.mass">Mass: <Kilogram :value="inventoryType.mass" /></li>
			<li v-if="inventoryType.metaGroupId">Meta group ID: {{ inventoryType.metaGroupId }}</li>
			<li v-if="inventoryType.packagedVolume">Packaged volume: <CubicMeters :value="inventoryType.packagedVolume" /></li>
			<li v-if="inventoryType.portionSize">Portion size: <FormattedNumber :number="inventoryType.portionSize" /></li>
			<li v-if="inventoryType.published">Published: {{ inventoryType.published }}</li>
			<li v-if="inventoryType.raceId">Race ID: {{ inventoryType.raceId }}</li>
			<li v-if="inventoryType.radius">Radius: <FormattedNumber :number="inventoryType.radius" /></li>
			<li v-if="inventoryType.sofFactionName">Sof faction name: {{ inventoryType.sofFactionName }}</li>
			<li v-if="inventoryType.sofMaterialSetId">Sof material set ID: {{ inventoryType.sofMaterialSetId }}</li>
			<li v-if="inventoryType.soundId">Sound ID: {{ inventoryType.soundId }}</li>
			<li v-if="inventoryType.volume">Volume: <FormattedNumber :number="inventoryType.volume" /></li>
			<DogmaListItems :inventory-type="inventoryType" :dogma-attributes="dogmaAttributes" />
		</ul>
	</CardWrapper>
</template>
