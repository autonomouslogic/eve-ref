<script setup lang="ts">
import refdataApi from "~/refdata";
import MarketGroupLink from "~/components/helpers/MarketGroupLink.vue";

const {locale} = useI18n();

const marketGroupIds: number[] = await refdataApi.getRootMarketGroups();
</script>

<template>
  <div>
    <h1>Market Groups</h1>
    <ul>
      <li v-for="marketGroupId in marketGroupIds">
        <MarketGroupLink :marketGroupId="marketGroupId"></MarketGroupLink>
      </li>
    </ul>
  </div>
</template>
