<script setup lang="ts">
import refdataApi, {cacheCategoriesBundle} from "~/refdata";
import CategoryLink from "~/components/helpers/CategoryLink.vue";
import {tr} from "~/lib/translate";
const {locale} = useI18n();

useHead({
	title: "Categories"
});

await cacheCategoriesBundle();

const categoryIds: number[] = await refdataApi.getAllCategories();
const categories = await Promise.all(categoryIds.map(async (categoryId) => await refdataApi.getCategory({categoryId})));
const sortedCategoryIds = computed(() => categories.sort((a, b) => {
	const an = tr(a.name, locale.value) || "";
	const bn = tr(b.name, locale.value) || "";
	return an.localeCompare(bn);
})
	.map((category) => category.categoryId)
	.filter((categoryId) => categoryId !== undefined));

</script>

<template>
	<div>
		<h1>Categories</h1>
		<div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
			<CategoryLink
				class="py-2"
				v-for="categoryId in sortedCategoryIds"
				:key="categoryId"
				:categoryId="categoryId">
			</CategoryLink>
		</div>
	</div>
</template>
