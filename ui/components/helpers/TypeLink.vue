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
      :to="`/types/${typeId}`">
    {{ type.name[locale] }}
  </NuxtLink>
</template>
