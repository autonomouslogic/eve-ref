<script setup lang="ts">
import refdataApi from "~/refdata";
import Currency from "~/components/helpers/Currency.vue";
import {InventoryType} from "~/refdata-openapi";
import Traits from "~/components/types/traits/Traits.vue";

const {locale} = useI18n();
const route = useRoute();
const typeId = route.params.typeId;

if (!typeId) {
  console.error('typeId is null');
}

const inventoryType: InventoryType = await refdataApi.getType({typeId});
</script>

<template>
  <div>
    <h2>id: {{ route.params.typeId }}</h2>
    <img :src="`https://images.evetech.net/types/${inventoryType.typeId}/icon`" alt="">

    <p>Name: {{ inventoryType.name[locale] }}</p>
    <p>Description: {{ inventoryType.description[locale] }}</p>
    <p>Price:
      <currency :price="inventoryType.basePrice"></currency>
    </p>
    <Traits :traits="inventoryType.traits"></Traits>
  </div>
</template>
