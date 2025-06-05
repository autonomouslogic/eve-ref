<script setup lang="ts">
import MetaGroupName from "~/components/helpers/MetaGroupName.vue";
import InternalLink from "~/components/helpers/InternalLink.vue";

const props = defineProps<{
	metaGroupId: number | undefined
}>();

if (props.metaGroupId === undefined) {
	throw new Error("metaGroupId is required");
}
</script>

<template>
	<InternalLink
		:to="`/meta-groups/${props.metaGroupId}`">
		<MetaGroupName :meta-group-id="metaGroupId" />
	</InternalLink>
</template>
