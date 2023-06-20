<script setup lang="ts">
import refdataApi from "~/refdata";
import {InventoryCategory} from "~/refdata-openapi";

const {categoryId} = defineProps<{
  categoryId: number | undefined
}>();

const {locale} = useI18n();

const category: InventoryCategory = await refdataApi.getCategory({categoryId});
</script>

<template>
  <NuxtLink
    v-if="category"
    class="underline hover:underline-offset-4 font-bold text-blue-500 hover:text-blue-700"
    :to="`/categories/${categoryId}`">
    {{ category.name[locale] }}
  </NuxtLink>
</template>
