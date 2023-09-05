<script setup lang="ts">
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";

const props = defineProps<{
	value: string | number | undefined
}>();

</script>

<template>
	<FormattedNumber :number="value * 100.0" :decimals="2" />%
</template>
