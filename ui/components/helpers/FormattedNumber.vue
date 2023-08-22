<script setup lang="ts">

const props = defineProps<{
	number: number | undefined,
	decimals: number | undefined
}>();

const realDecimals = computed(() => props.decimals ?? 0);
const formattedNumber = computed(() => typeof props.number === "number"
	? new Intl.NumberFormat("en-US", {
		minimumFractionDigits: realDecimals.value,
		maximumFractionDigits: realDecimals.value
	}).format(props.number)
	: "?"
);

</script>

<template>{{ formattedNumber }}</template>
