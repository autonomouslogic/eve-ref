<template>
  <div>
    <h2>id: {{ $route.params.id }}</h2>
    <img :src="`https://images.evetech.net/types/${inventoryType.type_id}/icon`" alt="">

    <p>Name: {{ inventoryType.name[locale] }}</p>
    <p>Description: {{ inventoryType.description[locale] }}</p>
    <p>Price: {{ inventoryType.base_price }}</p>
  </div>
</template>

<script setup lang="ts">
import {getInventoryType} from "~/refdata";

  const { locale } = useI18n();
  const route = useRoute();
  const id = route.params.id;

  if (!id) {
    console.error('id is null');
  }

  const inventoryType = await getInventoryType(parseInt(id));
</script>

<style scoped>

</style>
