<script setup lang="ts">
const props = defineProps<{
	title: string | undefined
}>();
</script>

<template>
	<div class="self-start w-full lg:w-auto overflow-clip">
		<h2 v-if="props.title">{{ props.title }}</h2>
		<slot />
	</div>
</template>

<style scoped>
div {
  @apply border border-gray-700 p-3 my-4;
  background-color: var(--card-background-color);
}
</style>
