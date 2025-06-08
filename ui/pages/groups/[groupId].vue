<script setup lang="ts">
import refdataApi, {cacheGroupBundle} from "~/refdata";
import {getIntRouteParam} from "~/lib/routeUtils";
import CategoryLink from "~/components/helpers/CategoryLink.vue";
import {getGroupDogma} from "~/lib/mainDogmaAttributes";
import {tr} from "~/lib/translate";
import GroupName from "~/components/helpers/GroupName.vue";

const route = useRoute();
const {locale} = useI18n();

const groupId = getIntRouteParam(route, "groupId");

await cacheGroupBundle(groupId);
const group = await refdataApi.getGroup({groupId});
useHead({
	title: tr(group.name, locale.value)
});
const attributes = await getGroupDogma(groupId);

const typeIds: number[] = group.typeIds?.filter((typeId) => typeId !== undefined) as number[];
const types = await Promise.all(typeIds.map(async (typeId) => await refdataApi.getType({typeId})));
const sortedTypeIds = computed(() => types.sort((a, b) => {
	const an = tr(a.name, locale.value) || "";
	const bn = tr(b.name, locale.value) || "";
	return an.localeCompare(bn);
})
	.map((type) => type.typeId)
	.filter((typeId) => typeId !== undefined));
</script>

<template>
	<div>
		<h1 v-if="group.name">{{ tr(group.name, locale) }}</h1>
		<div class="mb-3">
			Category: <CategoryLink :category-id="group.categoryId" /> &gt; <GroupName :group-id="group.groupId" />
		</div>
		<CompareTable
			:type-ids="sortedTypeIds"
			:dogma-attribute-names="attributes || []"
			direction="vertical"
			:compact-attribute-names="true"
			:show-meta-group="true"
			:load-bundles="false" />
	</div>
</template>
