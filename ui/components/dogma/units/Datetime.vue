<script setup lang="ts">

import {formatDuration} from "~/lib/timeUtils";
import {DateTime} from "luxon";

const props = defineProps<{
	millisecondEpoch: number
}>();

const formatted = computed(() => {
	return DateTime.fromMillis(props.millisecondEpoch).toUTC().toFormat("yyyy-MM-dd HH:mm:ss z");
});

</script>

<template>
	{{ formatted }}
</template>
