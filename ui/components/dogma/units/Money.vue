<script setup lang="ts">
import {formatMoney} from "~/lib/money";

const props = defineProps<{
	value: number
}>();

const formatted = computed(() => {
	return formatMoney(props.value);
});
</script>

<template>
	<span class="whitespace-nowrap">{{ formatted }}</span>
</template>
