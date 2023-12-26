<script setup lang="ts">
import {DogmaAttribute, InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import DogmaListItems from "~/components/cards/DogmaListItems.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import MarketGroupLink from "~/components/helpers/MarketGroupLink.vue";
import UnitValue from "~/components/dogma/UnitValue.vue";
import GroupLink from "~/components/helpers/GroupLink.vue";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();
</script>

<template>
	<CardWrapper title="Basics">
		<dl class="attribute-list">
			<template v-if="inventoryType.typeId">
				<dt>Type ID:</dt>
				<dd>{{ inventoryType.typeId }}</dd>
			</template>
			<template v-if="inventoryType.basePrice">
				<dt>Base price:</dt>
				<dd><UnitValue :unit-id="133" :value="inventoryType.basePrice" /></dd>
			</template>
			<template v-if="inventoryType.capacity">
				<dt>Capacity:</dt>
				<dd><UnitValue :unit-id="9" :value="inventoryType.capacity" /></dd>
			</template>
			<template v-if="inventoryType.factionId">
				<dt>Faction ID:</dt>
				<dd>{{ inventoryType.factionId }}</dd>
			</template>
			<template v-if="inventoryType.graphicId">
				<dt>Graphic ID:</dt>
				<dd>{{ inventoryType.graphicId }}</dd>
			</template>
			<template v-if="inventoryType.groupId">
				<dt>Group ID:</dt>
				<dd><GroupLink :group-id="inventoryType.groupId"/> [{{ inventoryType.groupId }}]</dd>
			</template>
			<template v-if="inventoryType.iconId">
				<dt>Icon ID:</dt>
				<dd>{{ inventoryType.iconId }}</dd>
			</template>
			<template v-if="inventoryType.marketGroupId">
				<dt>Market group ID:</dt>
				<dd><MarketGroupLink :market-group-id="inventoryType.marketGroupId"/> [{{ inventoryType.marketGroupId }}]</dd>
			</template>
			<template v-if="inventoryType.mass">
				<dt>Mass:</dt>
				<dd><UnitValue :unit-id="2" :value="inventoryType.mass" /></dd>
			</template>
			<template v-if="inventoryType.metaGroupId">
				<dt>Meta group ID:</dt>
				<dd>{{ inventoryType.metaGroupId }}</dd>
			</template>
			<template v-if="inventoryType.packagedVolume">
				<dt>Packaged volume:</dt>
				<dd><UnitValue :unit-id="9" :value="inventoryType.packagedVolume" /></dd>
			</template>
			<template v-if="inventoryType.portionSize">
				<dt>Portion size:</dt>
				<dd><FormattedNumber :number="inventoryType.portionSize" /></dd>
			</template>
			<template v-if="inventoryType.published">
				<dt>Published:</dt>
				<dd>{{ inventoryType.published }}</dd>
			</template>
			<template v-if="inventoryType.raceId">
				<dt>Race ID:</dt>
				<dd>{{ inventoryType.raceId }}</dd>
			</template>
			<template v-if="inventoryType.radius">
				<dt>Radius:</dt>
				<dd><UnitValue :unit-id="1" :value="inventoryType.radius" /></dd>
			</template>
			<template v-if="inventoryType.sofFactionName">
				<dt>Sof faction name:</dt>
				<dd>{{ inventoryType.sofFactionName }}</dd>
			</template>
			<template v-if="inventoryType.sofMaterialSetId">
				<dt>Sof material set ID:</dt>
				<dd>{{ inventoryType.sofMaterialSetId }}</dd>
			</template>
			<template v-if="inventoryType.soundId">
				<dt>Sound ID:</dt>
				<dd>{{ inventoryType.soundId }}</dd>
			</template>
			<template v-if="inventoryType.volume">
				<dt>Volume:</dt>
				<dd><UnitValue :unit-id="9" :value="inventoryType.volume" /></dd>
			</template>
			<DogmaListItems :inventory-type="inventoryType" :dogma-attributes="dogmaAttributes" />
		</dl>
	</CardWrapper>
</template>
