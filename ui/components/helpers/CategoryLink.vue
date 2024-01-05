<script setup lang="ts">
import refdataApi from "~/refdata";
import {type InventoryCategory} from "~/refdata-openapi";

const props = defineProps<{
	categoryId: number | undefined
}>();

const {locale} = useI18n();

if (props.categoryId === undefined) {
	throw new Error("categoryId is required");
}

const category: InventoryCategory = await refdataApi.getCategory({categoryId: props.categoryId});
</script>

<template>
	<NuxtLink
		v-if="category && category.name"
		:to="`/categories/${props.categoryId}`">
		{{ category.name[locale] }}
	</NuxtLink>
	<span v-else>(Unknown category ID {{props.categoryId}})</span>
</template>
