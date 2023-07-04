<script setup lang="ts">
import {InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import FormattedCurrency from "~/components/helpers/FormattedCurrency.vue";

const props = defineProps<{
	inventoryType: InventoryType
}>();
</script>

<template>
	<CardWrapper title="Basics">
		<ul class="list-disc list-inside">
			<li v-if="inventoryType.typeId">Type ID: {{ inventoryType.typeId }}</li>
			<li v-if="inventoryType.basePrice">Base price: <FormattedCurrency :price="inventoryType.basePrice" /></li>
			<li v-if="inventoryType.capacity">Capacity: {{ inventoryType.capacity }}</li>
			<li v-if="inventoryType.factionId">Faction ID: {{ inventoryType.factionId }}</li>
			<li v-if="inventoryType.graphicId">Graphic ID: {{ inventoryType.graphicId }}</li>
			<li v-if="inventoryType.groupId">Group ID: {{ inventoryType.groupId }}</li>
			<li v-if="inventoryType.iconId">Icon ID: {{ inventoryType.iconId }}</li>
			<li v-if="inventoryType.marketGroupId">Market group ID: {{ inventoryType.marketGroupId }}</li>
			<li v-if="inventoryType.mass">Mass: {{ inventoryType.mass }}</li>
			<li v-if="inventoryType.metaGroupId">Meta group ID: {{ inventoryType.metaGroupId }}</li>
			<li v-if="inventoryType.packagedVolume">Packaged volume: {{ inventoryType.packagedVolume }}</li>
			<li v-if="inventoryType.portionSize">Portion size: {{ inventoryType.portionSize }}</li>
			<li v-if="inventoryType.published">Published: {{ inventoryType.published }}</li>
			<li v-if="inventoryType.raceId">Race ID: {{ inventoryType.raceId }}</li>
			<li v-if="inventoryType.radius">Radius: {{ inventoryType.radius }}</li>
			<li v-if="inventoryType.sofFactionName">Sof faction name: {{ inventoryType.sofFactionName }}</li>
			<li v-if="inventoryType.sofMaterialSetId">Sof material set ID: {{ inventoryType.sofMaterialSetId }}</li>
			<li v-if="inventoryType.soundId">Sound ID: {{ inventoryType.soundId }}</li>
			<li v-if="inventoryType.volume">Volume: {{ inventoryType.volume }}</li>
		</ul>
	</CardWrapper>
</template>
