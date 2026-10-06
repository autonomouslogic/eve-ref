<script setup lang="ts">
import {scaleLinear} from "d3-scale";
import {select} from "d3-selection";
import {zoom, zoomIdentity, type D3ZoomEvent} from "d3-zoom";
import mapRegionsData from "~/assets/data/map-regions.json";
import InternalLink from "~/components/helpers/InternalLink.vue";

interface MapRegion {
	name: string;
	position: {x: number; y: number; z: number};
	region_id: number;
	universe_id: string;
}

const CONTAINER_WIDTH = 1000;
const CONTAINER_HEIGHT = (CONTAINER_WIDTH * 3) / 4;
const BOX_WIDTH = 140;
const BOX_HEIGHT = 36;

const regions = (mapRegionsData as MapRegion[]).filter((region) => region.universe_id === "eve");

const xExtent = [Math.min(...regions.map((r) => r.position.x)), Math.max(...regions.map((r) => r.position.x))];
const yExtent = [Math.min(...regions.map((r) => r.position.y)), Math.max(...regions.map((r) => r.position.y))];

const containerHeight = CONTAINER_HEIGHT;

// Independent x/y scales so the data fills a fixed 4:3 box (distorts relative distances).
const xScale = scaleLinear().domain(xExtent).range([0, CONTAINER_WIDTH - BOX_WIDTH]);
const yScale = scaleLinear().domain(yExtent).range([0, containerHeight - BOX_HEIGHT]);

const positionedRegions = regions.map((region) => ({
	...region,
	left: xScale(region.position.x),
	top: yScale(region.position.y)
}));

const viewportRef = ref<HTMLElement | null>(null);
const transform = ref(zoomIdentity);

onMounted(() => {
	if (!viewportRef.value) {
		return;
	}

	const zoomBehavior = zoom<HTMLElement, unknown>()
		.scaleExtent([0.2, 8])
		.on("zoom", (event: D3ZoomEvent<HTMLElement, unknown>) => {
			transform.value = event.transform;
		});

	select(viewportRef.value).call(zoomBehavior);
});

const innerStyle = computed(() => ({
	width: `${CONTAINER_WIDTH}px`,
	height: `${containerHeight}px`,
	transform: `translate(${transform.value.x}px, ${transform.value.y}px) scale(${transform.value.k})`,
	transformOrigin: "0 0"
}));
</script>

<template>
	<div
		ref="viewportRef"
		class="regions-map-viewport">
		<div
			class="regions-map-inner"
			:style="innerStyle">
			<div
				v-for="region in positionedRegions"
				:key="region.region_id"
				class="regions-map-box"
				:style="{left: `${region.left}px`, top: `${region.top}px`, width: `${BOX_WIDTH}px`, height: `${BOX_HEIGHT}px`}">
				<InternalLink :to="`/regions/${region.region_id}`">
					{{ region.name }}
				</InternalLink>
			</div>
		</div>
	</div>
</template>

<style scoped>
.regions-map-viewport {
	position: relative;
	width: 100%;
	height: 80vh;
	overflow: hidden;
	border: 1px solid #ccc;
	touch-action: none;
}

.regions-map-inner {
	position: relative;
}

.regions-map-box {
	position: absolute;
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 4px 8px;
	border: 1px solid #888;
	background: rgba(0, 0, 0, 0.05);
	border-radius: 4px;
	text-align: center;
	font-size: 0.75rem;
	overflow: hidden;
	white-space: nowrap;
}
</style>
