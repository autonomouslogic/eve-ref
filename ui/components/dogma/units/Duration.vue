<script setup lang="ts">

import {formatDuration} from "~/lib/timeUtils";

const props = defineProps<{
	milliseconds: number | undefined
}>();

const formatted = computed(() => {
	const ms = props.milliseconds;
	return formatDuration(ms === undefined ? 0 : ms);
});

</script>

<template>
	{{ formatted }}
</template>
