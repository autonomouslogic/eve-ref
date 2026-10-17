<script setup lang="ts">
import refdataApi from "~/refdata";
import RegionLink from "~/components/helpers/RegionLink.vue";
import RegionsMap from "~/components/regions/RegionsMap.vue";
import {tr} from "~/lib/translate";

const {locale} = useI18n();

useHead({
	title: "Regions"
});

const regionIds: number[] = await refdataApi.getAllRegions();

const regions = await Promise.all(regionIds.map(async (regionId) => await refdataApi.getRegion({regionId})));
const sortedRegions = computed(() => regions.sort((a, b) => {
	const an = tr(a.name, locale.value) || "";
	const bn = tr(b.name, locale.value) || "";
	return an.localeCompare(bn);
}));

const view = ref<"table" | "map">("table");
</script>

<template>
	<div>
		<h1 class="mb-3">Regions</h1>

		<div class="mb-3 flex gap-2">
			<button
				type="button"
				class="px-3 py-1 rounded border"
				:class="view === 'table' ? 'bg-black text-white' : 'bg-white'"
				@click="view = 'table'">
				Table
			</button>
			<button
				type="button"
				class="px-3 py-1 rounded border"
				:class="view === 'map' ? 'bg-black text-white' : 'bg-white'"
				@click="view = 'map'">
				Map
			</button>
		</div>

		<table
			v-if="view === 'table'"
			class="standard-table">
			<thead>
				<tr>
					<th>Region</th>
					<th class="text-right">Solar Systems</th>
				</tr>
			</thead>
			<tbody>
				<tr v-for="region in sortedRegions" :key="region.regionId">
					<td>
						<RegionLink :region-id="region.regionId" />
					</td>
					<td class="text-right">{{ region.solarSystemIds?.length || 0 }}</td>
				</tr>
			</tbody>
		</table>

		<RegionsMap v-else />
	</div>
</template>
