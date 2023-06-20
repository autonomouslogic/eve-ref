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
  <a href="/groups/{{ group.groupId }}">{{ group.name[locale] }}</a>
</template>
