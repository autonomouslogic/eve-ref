<script setup lang="ts">
import refdataApi from "~/refdata";
import Currency from "~/components/helpers/Currency.vue";
import {InventoryGroup, InventoryType} from "~/refdata-openapi";
import Traits from "~/components/types/traits/Traits.vue";
import CategoryLink from "~/components/helpers/CategoryLink.vue";
import GroupLink from "~/components/helpers/GroupLink.vue";
import MarketGroupBreadcrumbs from "~/components/helpers/MarketGroupBreadcrumbs.vue";

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
    <p v-if="inventoryType.marketGroupId">
      <MarketGroupBreadcrumbs :market-group-id="inventoryType.marketGroupId"></MarketGroupBreadcrumbs>
    </p>
    <img :src="`https://images.evetech.net/types/${inventoryType.typeId}/icon`" alt="">

    <p>Type ID: {{ route.params.typeId }}</p>
    <p>Description: {{ inventoryType.description[locale] }}</p>
    <p>Price:
      <currency :price="inventoryType.basePrice"></currency>
    </p>
    <Traits :traits="inventoryType.traits"></Traits>

    <h2>Dogma values</h2>
    <ul>
      <li v-for="attributeValue in inventoryType.dogmaAttributes">
        <DogmaAttributeValue :value="attributeValue.value" :attribute-id="attributeValue.attributeId" />
      </li>
    </ul>
  </div>
</template>