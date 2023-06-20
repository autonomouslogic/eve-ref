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
    class="underline hover:underline-offset-4 font-bold text-blue-500 hover:text-blue-700"
    :to="`/groups/${groupId}`">
    {{ group.name[locale] }}
  </NuxtLink>
</template>
