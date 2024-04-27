<script setup lang="ts">

const props = defineProps<{
	value: number
}>();

const format = new Intl.NumberFormat("en-US", {
	minimumFractionDigits: 2,
	maximumFractionDigits: 2
});

const formatted = computed(() => {
	let v = props.value;
	let s = "";
	if (v > 1.5e9) {
		v = props.value / 1e9;
		s = "b";
	}
	else if (v > 1.5e6) {
		v = props.value / 1e6;
		s = "m";
	}
	return `${format.format(v)}${s} ISK`;
});

</script>

<template>
	<span class="whitespace-nowrap">{{ formatted }}</span>
</template>
