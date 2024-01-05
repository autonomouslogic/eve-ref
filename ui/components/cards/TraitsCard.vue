<script setup lang="ts">
import {type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import TraitsContainer from "~/components/types/traits/TraitsContainer.vue";

const props = defineProps<{
	inventoryType: InventoryType
}>();
</script>

<template>
	<CardWrapper title="Traits" v-if="inventoryType.traits">
		<TraitsContainer :traits="inventoryType.traits"></TraitsContainer>
	</CardWrapper>
</template>
