<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import ExternalLink from "~/components/helpers/ExternalLink.vue";

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
		<ExternalLink
			v-if="!inventoryType.isBlueprint"
			:url="`https://www.adam4eve.eu/material_influence.php?material=${inventoryType.typeId}`">Adam4Eve Material Influence</ExternalLink>
	</CardWrapper>
</template>
