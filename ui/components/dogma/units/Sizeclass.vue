<script setup lang="ts">

const props = defineProps<{
	value: number
}>();
</script>

<template>
	<template v-if="value == 1">small</template>
	<template v-else-if="value == 2">medium</template>
	<template v-else-if="value == 3">large</template>
	<template v-else-if="value == 4">x-large</template>
	<template v-else>{{ value }}</template>
</template>
