<script setup lang="ts">
import refdataApi from "~/refdata";
import GroupLink from "~/components/helpers/GroupLink.vue";
import {getIntRouteParam} from "~/lib/routeUtils";
import {prepMessages} from "~/lib/translate";

const route = useRoute();
const {locale} = useI18n();

const categoryId = getIntRouteParam(route, "categoryId");
const category = await refdataApi.getCategory({categoryId});
useHead({
	title: prepMessages(category.name)[locale.value]
});
const groupIds = category?.groupIds;
</script>

<template>
	<div v-if="category">
		<h1 v-if="category.name">{{ prepMessages(category.name)[locale] }}</h1>
		<ul>
			<li v-for="groupId in groupIds" :key="groupId">
				<GroupLink :groupId="groupId"></GroupLink>
			</li>
		</ul>
	</div>
	<div v-else>(Unknown category ID {{ categoryId }})</div>
</template>
