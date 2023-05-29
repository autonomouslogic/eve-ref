<script setup lang="ts">
  const route = useRoute();
</script>

<template>
  <div>
    navbar - {{ route.path }}
  </div>
  <hr>
</template>

<style scoped>

</style>