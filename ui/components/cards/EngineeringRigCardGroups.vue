<script setup lang="ts">
import GroupLink from "~/components/helpers/GroupLink.vue";
import AttributeListItem from "~/components/attr/AttributeListItem.vue";

const props = defineProps<{
	name: string,
	groups: number[] | undefined
}>();
</script>

<template>
	<AttributeListItem v-if="groups">
		<template v-slot:key>
			Affected groups ({{ name }}):
		</template>
		<div v-for="groupId in groups" :key="groupId">
			<GroupLink :group-id="groupId" />
		</div>
	</AttributeListItem>
</template>
