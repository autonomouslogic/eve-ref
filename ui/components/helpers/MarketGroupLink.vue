<script setup lang="ts">
import MarketGroupName from "~/components/helpers/MarketGroupName.vue";

const props = defineProps<{
	marketGroupId: number | undefined
}>();

if (props.marketGroupId === undefined) {
	throw new Error("marketGroupId is required");
}
</script>

<template>
	<NuxtLink
		:to="`/market-groups/${props.marketGroupId}`">
		<MarketGroupName :market-group-id="marketGroupId" />
	</NuxtLink>
</template>
