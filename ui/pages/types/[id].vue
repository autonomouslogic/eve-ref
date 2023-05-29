<template>
  <div>
    <h2>id: {{ $route.params.id }}</h2>
    <img :src="`https://images.evetech.net/types/${_data.type_id}/icon`" alt="">
    <p>Name: {{ _data.name.en }}</p>
    <p>Description: {{ _data.description.en }}</p>
    <p>Price: {{ _data.base_price }}</p>
  </div>
</template>

<script setup lang="ts">
  import {Type} from "~/types/type";

  const route = useRoute();
  const id = route.params.id;

  if (!id) {
    console.error('id is null');
  }

  const { data } = await useFetch(`https://ref-data.everef.net/types/${id}`);
  const _data: Type = data;

  // get type data
  // set a global language
</script>

<style scoped>

</style>