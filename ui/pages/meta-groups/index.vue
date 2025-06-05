<script setup lang="ts">
import refdataApi, {cacheRootMarketGroupBundle} from "~/refdata";
import MarketGroupLink from "~/components/helpers/MarketGroupLink.vue";
import {tr} from "~/lib/translate";
import MetaGroupLink from "~/components/helpers/MetaGroupLink.vue";

const {locale} = useI18n();

// await cacheRootMetaGroupBundle();
useHead({
	title: "Meta Groups"
});

const metaGroupIds: number[] = await refdataApi.getAllMetaGroups();

const groups = await Promise.all(metaGroupIds.map(async (metaGroupId) => await refdataApi.getMetaGroup({metaGroupId})));
const sortedGroupIds = computed(() => groups.sort((a, b) => {
	const an = tr(a.name, locale.value) || "";
	const bn = tr(b.name, locale.value) || "";
	return an.localeCompare(bn);
})
	.map((group) => group.metaGroupId)
	.filter((groupId) => groupId !== undefined));
</script>

<template>
	<div>
		<h1>Meta Groups</h1>
		<div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
			<MetaGroupLink
				class="py-2"
				v-for="metaGroupId in sortedGroupIds"
				:key="metaGroupId"
				:meta-group-id="metaGroupId">
			</MetaGroupLink>
		</div>
	</div>
</template>
