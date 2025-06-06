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
		<h1 class="mb-3">Categories</h1>

		<table class="standard-table">
			<thead>
				<tr>
					<th>Category</th>
				</tr>
			</thead>
			<tbody>
				<tr v-for="categoryId in sortedCategoryIds" :key="categoryId">
					<td>
						<CategoryLink :categoryId="categoryId" />
					</td>
				</tr>
			</tbody>
		</table>
	</div>
</template>
