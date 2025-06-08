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
	<span
		v-if="group && group.name">
		{{ tr(group.name, locale) }}
	</span>
	<span v-else>(Unknown group ID {{props.groupId}})</span>
</template>
