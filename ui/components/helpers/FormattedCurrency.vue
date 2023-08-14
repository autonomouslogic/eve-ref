<script setup lang="ts">

import FormattedNumber from "~/components/helpers/FormattedNumber.vue";

const props = defineProps<{
	price: number | undefined
}>();

const formattedPrice = computed(() => props.price ? new Intl.NumberFormat("en-US").format(props.price) : "?");
</script>

<template><FormattedNumber :number="price" /> ISK</template>
