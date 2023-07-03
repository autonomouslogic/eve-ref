<script setup lang="ts">
import refdataApi from "~/refdata";
import {InventoryGroup} from "~/refdata-openapi";

const props = defineProps<{
  groupId: number | undefined
}>();

const {locale} = useI18n();

const group: InventoryGroup = await refdataApi.getGroup({groupId: props.groupId});
</script>

<template>
  <NuxtLink
    v-if="group"
    :to="`/groups/${props.groupId}`">
    {{ group.name[locale] }}
  </NuxtLink>
  <span v-else>(Unknown group ID {{props.groupId}})</span>
</template>
