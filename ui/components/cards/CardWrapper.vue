<script setup lang="ts">
const props = defineProps<{
	title: string | undefined;
	fullWidth?: boolean;
}>();

const cardSize: string = props.fullWidth ? "w-full" : "flex-auto";
</script>

<template>
	<div class="overflow-hidden" :class="cardSize">
		<h2 v-if="props.title">{{ props.title }}</h2>
		<slot />
	</div>
</template>

<style scoped>
div {
  @apply border border-gray-700 p-3;
  background-color: var(--card-background-color);
}
</style>
