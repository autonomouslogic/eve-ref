<script setup lang="ts">
import refdataApi from "~/refdata";
import {type InventoryGroup} from "~/refdata-openapi";
import {tr} from "~/lib/translate";
import InternalLink from "~/components/helpers/InternalLink.vue";

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
	<InternalLink
		v-if="group && group.name"
		:to="`/groups/${props.groupId}`">
		{{ tr(group.name, locale) }}
	</InternalLink>
	<span v-else>(Unknown group ID {{props.groupId}})</span>
</template>
