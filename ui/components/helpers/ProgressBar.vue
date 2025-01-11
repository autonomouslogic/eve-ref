<script setup lang="ts">
const props = defineProps<{
	progress: number,
	color?: string
}>();

const progressStyle = computed(() => {
	const realColor = props.color || "blue";
	const realProgress = Math.max(0, Math.min(1, props.progress));
	return `width: ${realProgress * 100}%; background-color: ${realColor};`;
});

</script>

<template>
	<div class="bar relative flex w-full">
		<div class="progress" :style="progressStyle"></div>
		<div class="content place-self-center"><slot /></div>
	</div>
</template>

<style scoped>
.bar {
  background-color: var(--body-background-color);
}
.content {
  position:absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%,-50%);
}
</style>
