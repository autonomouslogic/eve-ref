<script setup lang="ts">
const {price} = defineProps<{
  price: number | undefined
}>();

const formattedPrice = computed(() => price ? new Intl.NumberFormat('en-US').format(price) : '?');
</script>

<template>
  {{ formattedPrice }} ISK
</template>
