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
const sortedCategories = computed(() => categories.sort((a, b) => {
	const an = tr(a.name, locale.value) || "";
	const bn = tr(b.name, locale.value) || "";
	return an.localeCompare(bn);
}));

</script>

<template>
	<div>
		<h1 class="mb-3">Categories</h1>

		<table class="standard-table">
			<thead>
				<tr>
					<th>Category</th>
					<th class="text-right">Groups</th>
				</tr>
			</thead>
			<tbody>
				<tr v-for="category in sortedCategories" :key="category.categoryId">
					<td>
						<CategoryLink :categoryId="category.categoryId" />
					</td>
					<td class="text-right">{{ category.groupIds?.length || 0 }}</td>
				</tr>
			</tbody>
		</table>
	</div>
</template>
