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
		<ul>
			<li v-for="categoryId in categoryIds" :key="categoryId">
				<CategoryLink :categoryId="categoryId"></CategoryLink>
			</li>
		</ul>
	</div>
</template>
