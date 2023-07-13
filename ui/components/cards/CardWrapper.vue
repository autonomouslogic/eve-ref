<script setup lang="ts">
const props = defineProps<{
	title: string | undefined
}>();
</script>

<template>
	<div class="block border-2 border-gray-400 p-3 bg-blue-100 rounded-lg my-4">
		<h2 v-if="props.title">{{ props.title }}</h2>
		<slot />
	</div>
</template>
