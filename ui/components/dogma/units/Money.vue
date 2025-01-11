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
	if (v >= 1e12) {
		v = props.value / 1e12;
		s = "t";
	}
	else if (v >= 1e9) {
		v = props.value / 1e9;
		s = "b";
	}
	else if (v >= 1e6) {
		v = props.value / 1e6;
		s = "m";
	}
	else if (v >= 1e3) {
		v = props.value / 1e3;
		s = "k";
	}
	return `${format.format(v)}${s} ISK`;
});

</script>

<template>
	<span class="whitespace-nowrap">{{ formatted }}</span>
</template>
