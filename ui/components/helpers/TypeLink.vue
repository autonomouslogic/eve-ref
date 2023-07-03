<script setup lang="ts">
import refdataApi from "~/refdata";
import {InventoryType} from "~/refdata-openapi";

const {locale} = useI18n();

const props = defineProps<{
  typeId: number | string | undefined
}>();

const type: InventoryType | undefined = props.typeId ? await refdataApi.getType({typeId: props.typeId}) : undefined;
</script>

<template>
  <NuxtLink
      v-if="type"
      :to="`/types/${props.typeId}`">
    {{ type.name[locale] }}
  </NuxtLink>
</template>
