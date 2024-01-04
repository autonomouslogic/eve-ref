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
	<span class="bar">
		<div class="progress" :style="progressStyle"></div>
		<div class="content"><slot /></div>
	</span>
</template>

<style scoped>
.bar {
  display: flex;
  position: relative;
  background-color: #272b30;
  /*flex: 1;*/
  height: 1rem;
  border-radius: .25rem;
  overflow: hidden;
}
.content {
  position:absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%,-50%);
  color: #f8f9fa;
  font-weight: 700;
}
</style>
