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
    class="underline hover:underline-offset-4 font-bold text-blue-500 hover:text-blue-700"
    :to="`/market-groups/${marketGroupId}`">
    {{ marketGroup.name[locale] }}
  </NuxtLink>
</template>
