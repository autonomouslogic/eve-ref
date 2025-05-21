<script setup lang="ts">
import AttributeListItem from "~/components/attr/AttributeListItem.vue";
import CategoryLink from "~/components/helpers/CategoryLink.vue";

const props = defineProps<{
	name: string,
	categories: number[] | undefined
}>();
</script>

<template>
	<AttributeListItem v-if="categories">
		<template v-slot:key>
			Affected categories ({{ name }}):
		</template>
		<div v-for="categoryId in categories" :key="categoryId">
			<CategoryLink :category-id="categoryId" />
		</div>
	</AttributeListItem>
</template>
