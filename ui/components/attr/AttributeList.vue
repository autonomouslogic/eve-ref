<script setup lang="ts">
interface Props {
	columns?: number
	elements?: number
}
const props = withDefaults(defineProps<Props>(), {
	columns: -1,
	elements: -1
});
const realColumns = computed(() => {
	if (props.columns > 0) {
		return props.columns;
	}
	if (props.elements < 1) {
		return 1;
	}
	return props.elements >= 10 ? 2 : 1;
});
</script>

<template>
	<dl :style="`columns: ${realColumns}`">
		<slot />
	</dl>
</template>

<style scoped>
dl {
}
</style>
