<script setup lang="ts">
import {formatNumber} from "~/lib/number";

const props = defineProps<{
	number: number | undefined,
	decimals?: number | undefined,
	minDecimals?: number | undefined,
	maxDecimals?: number | undefined
}>();

const realMinDecimals = computed(() => {
	if (props.minDecimals !== undefined) {
		return props.minDecimals;
	}
	return props.decimals ?? 0;
});
const realMaxDecimals = computed(() => {
	if (props.maxDecimals !== undefined) {
		return props.maxDecimals;
	}
	return props.decimals ?? 5;
});

const formattedNumber = computed(() => formatNumber(props.number, realMinDecimals.value, realMaxDecimals.value));

</script>

<template>
	<span class="whitespace-nowrap">{{ formattedNumber }}</span>
</template>
