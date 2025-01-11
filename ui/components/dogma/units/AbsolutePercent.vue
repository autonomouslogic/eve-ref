<script setup lang="ts">
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";

const props = defineProps<{
	value: number
}>();
</script>

<template>
	<FormattedNumber :number="value * 100.0" />%
</template>
