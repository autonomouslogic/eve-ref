<script setup lang="ts">
import refdataApi from "~/refdata";
import {InventoryGroup} from "~/refdata-openapi";

const {groupId} = defineProps<{
  groupId: number | undefined
}>();

const {locale} = useI18n();

const group: InventoryGroup = await refdataApi.getGroup({groupId});
</script>

<template>
  <NuxtLink
    v-if="group"
    :to="`/groups/${groupId}`">
    {{ group.name[locale] }}
  </NuxtLink>
</template>
