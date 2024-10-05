<script setup lang="ts">
import refdataApi, {cacheCategoryBundle} from "~/refdata";
import GroupLink from "~/components/helpers/GroupLink.vue";
import {getIntRouteParam} from "~/lib/routeUtils";
import {tr} from "~/lib/translate";

const route = useRoute();
const {locale} = useI18n();
const categoryId = getIntRouteParam(route, "categoryId");

await cacheCategoryBundle(categoryId);

const category = await refdataApi.getCategory({categoryId});
useHead({
	title: tr(category.name, locale.value),
});

const groupIds: number[] = category.groupIds?.filter((id) => id !== undefined) as number[];
const groups = await Promise.all(groupIds.map(async (groupId) => await refdataApi.getGroup({groupId})));
const sortedGroupIds = computed(() => groups.sort((a, b) => {
	const an = tr(a.name, locale.value) || "";
	const bn = tr(b.name, locale.value) || "";
	return an.localeCompare(bn);
})
	.map((group) => group.groupId));

</script>

<template>
	<div v-if="category">
		<h1 v-if="category.name">{{ tr(category.name, locale) }}</h1>
		<div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
			<GroupLink
				class="py-2"
				v-for="groupId in sortedGroupIds"
				:key="groupId"
				:groupId="groupId">
			</GroupLink>
		</div>
	</div>
	<div v-else>(Unknown category ID {{ categoryId }})</div>
</template>
