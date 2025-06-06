<script setup lang="ts">
import refdataApi, {cacheRootMarketGroupBundle} from "~/refdata";
import MarketGroupLink from "~/components/helpers/MarketGroupLink.vue";
import {tr} from "~/lib/translate";
import MetaGroupLink from "~/components/helpers/MetaGroupLink.vue";
import CategoryLink from "~/components/helpers/CategoryLink.vue";

const {locale} = useI18n();

// await cacheRootMetaGroupBundle();
useHead({
	title: "Meta Groups"
});

const metaGroupIds: number[] = await refdataApi.getAllMetaGroups();

const groups = await Promise.all(metaGroupIds.map(async (metaGroupId) => await refdataApi.getMetaGroup({metaGroupId})));
const sortedGroups = computed(() => groups.sort((a, b) => {
	const an = tr(a.name, locale.value) || "";
	const bn = tr(b.name, locale.value) || "";
	return an.localeCompare(bn);
}));
</script>

<template>
	<div>
		<h1 class="mb-3">Meta Groups</h1>

		<table class="standard-table">
			<thead>
				<tr>
					<th>Meta group</th>
					<th class="text-right">Types</th>
				</tr>
			</thead>
			<tbody>
				<tr v-for="group in sortedGroups" :key="group.metaGroupId">
					<td>
						<MetaGroupLink :meta-group-id="group.metaGroupId" />
					</td>
					<td class="text-right">{{ group.typeIds?.length || 0 }}</td>
				</tr>
			</tbody>
		</table>
	</div>
</template>
