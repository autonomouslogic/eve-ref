<script setup lang="ts">
import refdataApi from "~/refdata";
import CategoryLink from "~/components/helpers/CategoryLink.vue";

const categoryIds: number[] = await refdataApi.getAllCategories();
useHead({
	title: "Categories"
});
</script>

<template>
	<div>
		<h1>Categories</h1>
		<div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
			<CategoryLink
				class="py-2"
				v-for="categoryId in categoryIds"
				:key="categoryId"
				:categoryId="categoryId">
			</CategoryLink>
		</div>
	</div>
</template>
