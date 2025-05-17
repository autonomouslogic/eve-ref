<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import DogmaListItems from "~/components/cards/DogmaListItems.vue";
import MarketGroupLink from "~/components/helpers/MarketGroupLink.vue";
import UnitValue from "~/components/dogma/UnitValue.vue";
import GroupLink from "~/components/helpers/GroupLink.vue";
import AttributeList from "~/components/attr/AttributeList.vue";
import AttributeListItem from "~/components/attr/AttributeListItem.vue";
import {CUBIC_METER, METER, MONEY} from "~/lib/unitConstants";
import ExternalLink from "~/components/helpers/ExternalLink.vue";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();
</script>

<template>
	<CardWrapper title="Type Info">

		<AttributeList :elements="17 + dogmaAttributes.length">
			<AttributeListItem v-if="inventoryType.typeId">
				<template v-slot:key>Type ID:</template>
				{{ inventoryType.typeId }}
			</AttributeListItem>
			<AttributeListItem v-if="inventoryType.basePrice">
				<template v-slot:key>Base price:</template>
				<UnitValue :unit-id="MONEY" :value="inventoryType.basePrice" />
			</AttributeListItem>
			<AttributeListItem v-if="inventoryType.capacity">
				<template v-slot:key>Capacity:</template>
				<UnitValue :unit-id="CUBIC_METER" :value="inventoryType.capacity" />
			</AttributeListItem>
			<AttributeListItem v-if="inventoryType.factionId">
				<template v-slot:key>Faction ID:</template>
				{{ inventoryType.factionId }}
			</AttributeListItem>
			<AttributeListItem v-if="inventoryType.graphicId">
				<template v-slot:key>Graphic ID:</template>
				{{ inventoryType.graphicId }}
			</AttributeListItem>
			<AttributeListItem v-if="inventoryType.groupId">
				<template v-slot:key>Group ID:</template>
				<GroupLink :group-id="inventoryType.groupId"/>
			</AttributeListItem>
			<AttributeListItem v-if="inventoryType.iconId">
				<template v-slot:key>Icon ID:</template>
				{{ inventoryType.iconId }}
			</AttributeListItem>
			<AttributeListItem v-if="inventoryType.marketGroupId">
				<template v-slot:key>Market group ID:</template>
				<MarketGroupLink :market-group-id="inventoryType.marketGroupId"/>
			</AttributeListItem>
			<AttributeListItem v-if="inventoryType.metaGroupId">
				<template v-slot:key>Meta group ID:</template>
				{{ inventoryType.metaGroupId }}
			</AttributeListItem>
			<AttributeListItem v-if="inventoryType.published">
				<template v-slot:key>Published:</template>
				{{ inventoryType.published }}
			</AttributeListItem>
			<AttributeListItem v-if="inventoryType.raceId">
				<template v-slot:key>Race ID:</template>
				{{ inventoryType.raceId }}
			</AttributeListItem>
			<AttributeListItem v-if="inventoryType.radius">
				<template v-slot:key>Radius:</template>
				<UnitValue :unit-id="METER" :value="inventoryType.radius" />
			</AttributeListItem>
			<AttributeListItem v-if="inventoryType.sofFactionName">
				<template v-slot:key>Sof faction name:</template>
				{{ inventoryType.sofFactionName }}
			</AttributeListItem>
			<AttributeListItem v-if="inventoryType.sofMaterialSetId">
				<template v-slot:key>Sof material set ID:</template>
				{{ inventoryType.sofMaterialSetId }}
			</AttributeListItem>
			<AttributeListItem v-if="inventoryType.soundId">
				<template v-slot:key>Sound ID:</template>
				{{ inventoryType.soundId }}
			</AttributeListItem>
			<AttributeListItem v-if="inventoryType.volume">
				<template v-slot:key>Volume:</template>
				<UnitValue :unit-id="CUBIC_METER" :value="inventoryType.volume" />
			</AttributeListItem>
			<AttributeListItem>
				<template v-slot:key>
					<ExternalLink url="https://docs.everef.net/datasets/reference-data">Reference Data</ExternalLink>:
				</template>
				<div>
					<ExternalLink :url="`https://ref-data.everef.net/types/${inventoryType.typeId}`">Type JSON</ExternalLink>
				</div>
				<div v-if="inventoryType.isBlueprint" >
					<ExternalLink :url="`https://ref-data.everef.net/blueprints/${inventoryType.typeId}`">Blueprint JSON</ExternalLink>
				</div>
				<div v-if="inventoryType.isSkill">
					<ExternalLink :url="`https://ref-data.everef.net/skills/${inventoryType.typeId}`">Skill JSON</ExternalLink>
				</div>
			</AttributeListItem>
			<AttributeListItem>
				<template v-slot:key>
					<ExternalLink url="https://esi.evetech.net/">ESI</ExternalLink>:
				</template>
				<ExternalLink :url="`https://esi.evetech.net/latest/universe/types/${inventoryType.typeId}/`">View JSON</ExternalLink>
			</AttributeListItem>
			<AttributeListItem>
				<template v-slot:key>
					<ExternalLink url="https://sde.jita.space/">SDE</ExternalLink>:
				</template>
				<ExternalLink :url="`https://sde.jita.space/latest/universe/types/${inventoryType.typeId}`">View JSON</ExternalLink>
			</AttributeListItem>

			<DogmaListItems :inventory-type="inventoryType" :dogma-attributes="dogmaAttributes" />

		</AttributeList>
	</CardWrapper>
</template>
