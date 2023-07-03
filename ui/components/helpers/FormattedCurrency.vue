<script setup lang="ts">
const props = defineProps<{
	price: number | undefined
}>();

const formattedPrice = computed(() => props.price ? new Intl.NumberFormat("en-US").format(props.price) : "?");
</script>

<template>
	{{ formattedPrice }} ISK
</template>
