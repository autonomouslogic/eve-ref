<script setup lang="ts">
import refdataApi from "~/refdata";
import {InventoryType} from "~/refdata-openapi";

const {locale} = useI18n();

const {typeId} = defineProps<{
  typeId: number | string | undefined
}>();

const type: InventoryType | undefined = typeId ? await refdataApi.getType({typeId}) : undefined;
</script>

<template>
  <NuxtLink
      v-if="type"
      class="underline hover:underline-offset-4 font-bold text-blue-500 hover:text-blue-700"
      :to="`/types/${typeId}`">
    {{ type.name[locale] }}
  </NuxtLink>
</template>