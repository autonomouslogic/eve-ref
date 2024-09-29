<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import refdataApi from "~/refdata";
import {SHIP} from "~/lib/categoryConstants";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

const group = !props.inventoryType.groupId ? undefined :
	await refdataApi.getGroup({groupId: props.inventoryType.groupId});
const isShip = group?.categoryId == SHIP;
</script>

<template>
	<CardWrapper :title="title" v-if="isShip">
		<ClientOnly>
			<CardsInsuranceCardInner :inventory-type="inventoryType" />
		</ClientOnly>
	</CardWrapper>
</template>
