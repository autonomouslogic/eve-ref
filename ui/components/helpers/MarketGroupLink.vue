<script setup lang="ts">
import refdataApi from "~/refdata";
import {MarketGroup} from "~/refdata-openapi";

const {marketGroupId} = defineProps<{
  marketGroupId: number | undefined
}>();

const {locale} = useI18n();

const marketGroup: MarketGroup = await refdataApi.getMarketGroup({marketGroupId});
</script>

<template>
  <NuxtLink
    v-if="marketGroup"
    :to="`/market-groups/${marketGroupId}`">
    {{ marketGroup.name[locale] }}
  </NuxtLink>
</template>
