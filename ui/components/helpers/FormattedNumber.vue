<script setup lang="ts">

const props = defineProps<{
	number: number | undefined
}>();

const formattedNumber = computed(() => props.number
	? new Intl.NumberFormat("en-US").format(props.number)
	: "?"
);
</script>

<template>{{ formattedNumber }}</template>
