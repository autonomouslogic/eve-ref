<script setup lang="ts">
import MetaGroupName from "~/components/helpers/MetaGroupName.vue";
import InternalLink from "~/components/helpers/InternalLink.vue";
import refdataApi from "~/refdata";

const props = defineProps<{
	metaGroupId: number | undefined
}>();

const metaGroup = props.metaGroupId === undefined ? undefined :
	await refdataApi.getMetaGroup({metaGroupId: props.metaGroupId});
</script>

<template>
	<InternalLink
		v-if="metaGroup && metaGroup.name"
		:to="`/meta-groups/${props.metaGroupId}`">
		<MetaGroupName :meta-group-id="metaGroupId" />
	</InternalLink>
</template>
