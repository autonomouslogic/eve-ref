<script setup lang="ts">
import AbsolutePercent from "~/components/dogma/units/AbsolutePercent.vue";

const props = defineProps<{
	value: number
}>();
</script>

<template>
	<AbsolutePercent :value="1 - value" />
</template>
