<script setup lang="ts">
import refdataApi from "~/refdata";
import {type InventoryGroup} from "~/refdata-openapi";
import {tr} from "~/lib/translate";
import InternalLink from "~/components/helpers/InternalLink.vue";
import GroupName from "~/components/helpers/GroupName.vue";

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
	<template v-if="group">
		<InternalLink
			:to="`/groups/${props.groupId}`">
			<GroupName :groupId="props.groupId" />
		</InternalLink>
	</template>
</template>
