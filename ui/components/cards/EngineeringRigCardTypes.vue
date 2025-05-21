<script setup lang="ts">
import AttributeListItem from "~/components/attr/AttributeListItem.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";

const props = defineProps<{
	name: string,
	types: number[] | undefined
}>();
</script>

<template>
	<AttributeListItem v-if="types">
		<template v-slot:key>
			Source rigs ({{ name }}):
		</template>
		<div v-for="typeId in types" :key="typeId">
			<TypeLink :type-id="typeId" />
		</div>
	</AttributeListItem>
</template>
