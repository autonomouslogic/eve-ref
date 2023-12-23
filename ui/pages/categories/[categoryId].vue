<script setup lang="ts">
import refdataApi from "~/refdata";
import GroupLink from "~/components/helpers/GroupLink.vue";

const route = useRoute();
const {locale} = useI18n();

const categoryId = parseInt(Array.isArray(route.params.categoryId) ? route.params.categoryId[0] : route.params.categoryId);
const category = await refdataApi.getCategory({categoryId});
const groupIds = category?.groupIds;
</script>

<template>
	<div v-if="category">
		<h1 v-if="category.name">{{ category.name[locale] }}</h1>
		<p>Groups:</p>
		<ul>
			<li v-for="groupId in groupIds" :key="groupId">
				<GroupLink :groupId="groupId"></GroupLink>
			</li>
		</ul>
	</div>
	<div v-else>(Unknown category ID {{ categoryId }})</div>
</template>
