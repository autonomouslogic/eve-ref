<script setup lang="ts">
const localePath = useLocalePath();

const props = defineProps<{
	to: string
}>();



</script>

<template>
	<NuxtLink
		:to="localePath(to)">
		<slot />
	</NuxtLink>
</template>
