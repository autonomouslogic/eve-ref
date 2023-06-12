<script setup lang="ts">
import refdataApi from "~/refdata";

const {locale} = useI18n();

const {typeId} = defineProps<{
  typeId: number | undefined
}>();

const type = await refdataApi.getType({typeId});
</script>

<template>
  <NuxtLink :to="`/types/${typeId}`">{{ type.name[locale] }}</NuxtLink>
</template>