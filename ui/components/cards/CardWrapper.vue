<script setup lang="ts">
const props = defineProps<{
	title: string | undefined
}>();
</script>

<template>
	<div class="block bg-black border p-3 border-gray-700 my-4">
		<h2 v-if="props.title">{{ props.title }}</h2>
		<slot />
	</div>
</template>
