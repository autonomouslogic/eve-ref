<script setup lang="ts">
const props = defineProps<{
	url: string
}>();
</script>

<template>
	<NuxtLink
		:to="url"
		rel="noopener"
		target="_blank"
		:external="true">
		<slot />
	</NuxtLink>
</template>
