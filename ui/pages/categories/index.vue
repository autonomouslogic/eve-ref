<script setup lang="ts">
import refdataApi from "~/refdata";
import CategoryLink from "~/components/types/CategoryLink.vue";

const {locale} = useI18n();

const categoryIds: number[] = await refdataApi.getAllCategories();
</script>

<template>
  <div>
    <h1>Categories</h1>
    <ul>
      <li v-for="categoryId in categoryIds">
        <CategoryLink :categoryId="categoryId"></CategoryLink>
      </li>
    </ul>
  </div>
</template>
