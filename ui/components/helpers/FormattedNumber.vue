<script setup lang="ts">

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

const maxNormal = 1e15;
const minNormal = 1/maxNormal;

const notation = computed(() => {
	if (props.number === undefined) {
		return "standard";
	}
	const n = Math.abs(props.number);
	if (n != 0 && (n >= maxNormal || n <= minNormal)) {
		return "engineering";
	}
	return "standard";
});

const formattedNumber = computed(() => typeof props.number === "number"
	? new Intl.NumberFormat("en-US", {
		minimumFractionDigits: realMinDecimals.value,
		maximumFractionDigits: realMaxDecimals.value,
		notation: notation.value
	}).format(props.number)
	: "?"
);

</script>

<template>
	<span class="whitespace-nowrap">{{ formattedNumber }}</span>
</template>
