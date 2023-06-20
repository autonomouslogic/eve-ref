<script setup lang="ts">
import refdataApi from "~/refdata";
import GroupLink from "~/components/types/GroupLink.vue";

const route = useRoute();
const {locale} = useI18n();

const categoryId = route.params.categoryId;
var category = await refdataApi.getCategory({categoryId});
var groupIds = category.groupIds
</script>

<template>
  <div>
    <h1>{{ category.name[locale] }}</h1>
    <p>Groups:</p>
    <ul>
      <li v-for="groupId in groupIds">
        <GroupLink :groupId="groupId"></GroupLink>
      </li>
    </ul>
  </div>
</template>
