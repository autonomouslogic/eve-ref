<script setup lang="ts">
import refdataApi from "~/refdata";
import GroupLink from "~/components/helpers/GroupLink.vue";
import {getIntRouteParam} from "~/lib/routeUtils";
import {tr} from "~/lib/translate";

const route = useRoute();
const {locale} = useI18n();

const categoryId = getIntRouteParam(route, "categoryId");
const category = await refdataApi.getCategory({categoryId});
useHead({
	title: tr(category.name, locale.value),
});
const groupIds = category?.groupIds;
</script>

<template>
	<div v-if="category">
		<h1 v-if="category.name">{{ tr(category.name, locale) }}</h1>
		<div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
			<GroupLink
				class="py-2"
				v-for="groupId in groupIds"
				:key="groupId"
				:groupId="groupId">
			</GroupLink>
		</div>
	</div>
	<div v-else>(Unknown category ID {{ categoryId }})</div>
</template>
