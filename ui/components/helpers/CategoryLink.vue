<script setup lang="ts">
import refdataApi from "~/refdata";
import {type InventoryCategory} from "~/refdata-openapi";
import {tr} from "~/lib/translate";
import InternalLink from "~/components/helpers/InternalLink.vue";

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
	<InternalLink
		v-if="category && category.name"
		:to="`/categories/${props.categoryId}`">
		{{ tr(category.name, locale) }}
	</InternalLink>
	<span v-else>(Unknown category ID {{props.categoryId}})</span>
</template>
