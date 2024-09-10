<script setup lang="ts">
const props = defineProps<{
	number: number | undefined,
	decimals?: number | undefined
}>();

const realDecimals = computed(() => props.decimals ?? 0);
const maxNormal = 1e15;
const minNormal = 1/maxNormal;
const formattedNumber = computed(() => typeof props.number === "number"
	? new Intl.NumberFormat("en-US", {
		minimumFractionDigits: realDecimals.value,
		maximumFractionDigits: realDecimals.value,
		notation: props.number != 0 && (props.number >= maxNormal || props.number <= minNormal) ? "engineering" : "standard"
	}).format(props.number)
	: "?"
);

</script>

<template>
	<span class="whitespace-nowrap">{{ formattedNumber }}</span>
</template>
