<script setup lang="ts">
import refdataApi from "~/refdata";
import TypeLink from "~/components/helpers/TypeLink.vue";
import {getIntRouteParam} from "~/lib/routeUtils";
import {tr} from "~/lib/translate";

const route = useRoute();
const {locale} = useI18n();
const regionId = getIntRouteParam(route, "regionId");

const region = await refdataApi.getRegion({regionId});
useHead({
	title: tr(region.name, locale.value),
});

const solarSystemIds: number[] = region.solarSystemIds?.filter((id) => id !== undefined) as number[];
const solarSystems = await Promise.all(solarSystemIds.map(async (solarSystemId) => await refdataApi.getSolarSystem({solarSystemId})));

const planetIds: number[] = solarSystems.flatMap((solarSystem) => solarSystem.planetIds || []);
const planets = await Promise.all(planetIds.map(async (planetId) => await refdataApi.getPlanet({planetId})));

const solarSystemsById = computed(() => {
	const map = new Map<number, typeof solarSystems[number]>();
	solarSystems.forEach((solarSystem) => {
		if (solarSystem.solarSystemId !== undefined) map.set(solarSystem.solarSystemId, solarSystem);
	});
	return map;
});

const sortedPlanets = computed(() => planets.slice().sort((a, b) => {
	const an = tr(solarSystemsById.value.get(a.solarSystemId!)?.name, locale.value) || "";
	const bn = tr(solarSystemsById.value.get(b.solarSystemId!)?.name, locale.value) || "";
	if (an !== bn) return an.localeCompare(bn);
	return (a.celestialIndex || 0) - (b.celestialIndex || 0);
}));
</script>

<template>
	<div v-if="region">
		<h1 v-if="region.name" class="mb-3">{{ tr(region.name, locale) }}</h1>

		<table class="standard-table">
			<thead>
				<tr>
					<th>Solar System</th>
					<th>Planet</th>
					<th>Type</th>
				</tr>
			</thead>
			<tbody>
				<tr v-for="planet in sortedPlanets" :key="planet.planetId">
					<td>{{ tr(solarSystemsById.get(planet.solarSystemId!)?.name, locale) }}</td>
					<td>{{ planet.celestialIndex }}</td>
					<td>
						<TypeLink :type-id="planet.typeId" />
					</td>
				</tr>
			</tbody>
		</table>
	</div>
	<div v-else>(Unknown region ID {{ regionId }})</div>
</template>
