<script setup lang="ts">

const props = defineProps<{
	value: number
}>();
</script>

<template>
	<template v-if="value == 0">false</template>
	<template v-else-if="value == 1">true</template>
	<template v-else>{{ value }}</template>
</template>
