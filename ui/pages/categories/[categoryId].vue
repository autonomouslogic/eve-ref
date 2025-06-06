<script setup lang="ts">
import refdataApi, {cacheCategoryBundle} from "~/refdata";
import GroupLink from "~/components/helpers/GroupLink.vue";
import {getIntRouteParam} from "~/lib/routeUtils";
import {tr} from "~/lib/translate";
import CategoryLink from "~/components/helpers/CategoryLink.vue";

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
const sortedGroups = computed(() => groups.sort((a, b) => {
	const an = tr(a.name, locale.value) || "";
	const bn = tr(b.name, locale.value) || "";
	return an.localeCompare(bn);
}));

</script>

<template>
	<div v-if="category">
		<h1 v-if="category.name" class="mb-3">{{ tr(category.name, locale) }}</h1>

		<table class="standard-table">
			<thead>
				<tr>
					<th>Group</th>
					<th class="text-right">Types</th>
				</tr>
			</thead>
			<tbody>
				<tr v-for="group in sortedGroups" :key="group.groupId">
					<td>
						<GroupLink :group-id="group.groupId" />
					</td>
					<td class="text-right">{{ group.typeIds?.length || 0 }}</td>
				</tr>
			</tbody>
		</table>
	</div>
	<div v-else>(Unknown category ID {{ categoryId }})</div>
</template>
