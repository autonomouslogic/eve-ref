<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import ExternalLink from "~/components/helpers/ExternalLink.vue";
import InternalLink from "~/components/helpers/InternalLink.vue";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

const showCard = computed(() => {
	return !props.inventoryType.isBlueprint;
});
</script>

<template>
	<CardWrapper :title="title" v-if="showCard">
		<ul>
			<li>
				<InternalLink :to="`/types/${inventoryType.typeId}/used-in`">
					View blueprints
				</InternalLink>
			</li>
			<li>
				<ExternalLink
					v-if="!inventoryType.isBlueprint"
					:url="`https://www.adam4eve.eu/material_influence.php?material=${inventoryType.typeId}`">Adam4Eve Material Influence</ExternalLink>
			</li>
		</ul>
	</CardWrapper>
</template>
