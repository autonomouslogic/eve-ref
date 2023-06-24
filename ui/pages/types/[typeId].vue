<script setup lang="ts">
import refdataApi from "~/refdata";
import Currency from "~/components/helpers/Currency.vue";
import {InventoryGroup, InventoryType} from "~/refdata-openapi";
import Traits from "~/components/types/traits/Traits.vue";
import CategoryLink from "~/components/types/CategoryLink.vue";
import GroupLink from "~/components/types/GroupLink.vue";

const {locale} = useI18n();
const route = useRoute();
const typeId = route.params.typeId;

if (!typeId) {
  console.error('typeId is null');
}

const inventoryType: InventoryType = await refdataApi.getType({typeId});
const inventoryGroup: InventoryGroup = await refdataApi.getGroup({groupId: inventoryType.groupId});
</script>

<template>
  <div>
    <h1>{{ inventoryType.name[locale] }}</h1>
    <p>
      <CategoryLink :categoryId="inventoryGroup.categoryId"></CategoryLink> &gt;
      <GroupLink :groupId="inventoryType.groupId"></GroupLink>
    </p>
    <img :src="`https://images.evetech.net/types/${inventoryType.typeId}/icon`" alt="">

    <p>Type ID: {{ route.params.typeId }}</p>
    <p>Description: {{ inventoryType.description[locale] }}</p>
    <p>Price:
      <currency :price="inventoryType.basePrice"></currency>
    </p>
    <Traits :traits="inventoryType.traits"></Traits>
  </div>
</template>
