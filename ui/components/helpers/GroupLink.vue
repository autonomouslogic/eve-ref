<script setup lang="ts">
import refdataApi from "~/refdata";
import {type InventoryGroup} from "~/refdata-openapi";

const props = defineProps<{
	groupId: number | undefined
}>();

const {locale} = useI18n();

if (props.groupId === undefined) {
	throw new Error("groupId is required");
}

const group: InventoryGroup = await refdataApi.getGroup({groupId: props.groupId});
</script>

<template>
	<NuxtLink
		v-if="group && group.name"
		:to="`/groups/${props.groupId}`">
		{{ group.name[locale] }}
	</NuxtLink>
	<span v-else>(Unknown group ID {{props.groupId}})</span>
</template>
