<script setup lang="ts">
import {scaleLinear} from "d3-scale";
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

const SEPARATION_PADDING = 4;
const SEPARATION_ITERATIONS = 300;

interface PositionedRegion extends MapRegion {
	rawLeft: number;
	rawTop: number;
	left: number;
	top: number;
}

/**
 * Pushes overlapping boxes apart along their axis of least overlap, a few pixels per iteration,
 * until none overlap (or iterations run out). Operates in place on `left`/`top`.
 */
function separateBoxes(boxes: PositionedRegion[]): void {
	for (let iteration = 0; iteration < SEPARATION_ITERATIONS; iteration++) {
		let anyOverlap = false;

		for (let i = 0; i < boxes.length; i++) {
			for (let j = i + 1; j < boxes.length; j++) {
				const a = boxes[i];
				const b = boxes[j];

				let dx = b.left - a.left;
				let dy = b.top - a.top;

				// Boxes sitting at (almost) the same spot: nudge apart on a random axis to break the tie.
				if (Math.abs(dx) < 0.01 && Math.abs(dy) < 0.01) {
					dx = Math.random() - 0.5;
					dy = Math.random() - 0.5;
				}

				const overlapX = BOX_WIDTH + SEPARATION_PADDING - Math.abs(dx);
				const overlapY = BOX_HEIGHT + SEPARATION_PADDING - Math.abs(dy);

				if (overlapX <= 0 || overlapY <= 0) {
					continue;
				}

				anyOverlap = true;

				// Push apart along whichever axis needs the smaller move to resolve the overlap.
				if (overlapX < overlapY) {
					const push = (overlapX / 2) * Math.sign(dx || 1);
					a.left -= push;
					b.left += push;
				} else {
					const push = (overlapY / 2) * Math.sign(dy || 1);
					a.top -= push;
					b.top += push;
				}
			}
		}

		if (!anyOverlap) {
			break;
		}
	}
}

const positionedRegions: PositionedRegion[] = regions.map((region) => {
	const rawLeft = xScale(region.position.x);
	const rawTop = yScale(region.position.y);
	return {...region, rawLeft, rawTop, left: rawLeft, top: rawTop};
});

separateBoxes(positionedRegions);
</script>

<template>
	<div class="regions-map-viewport">
		<div
			class="regions-map-inner"
			:style="{width: `${CONTAINER_WIDTH}px`, height: `${containerHeight}px`}">
			<svg
				class="regions-map-leaders"
				:width="CONTAINER_WIDTH"
				:height="containerHeight">
				<line
					v-for="region in positionedRegions"
					:key="region.region_id"
					:x1="region.rawLeft + BOX_WIDTH / 2"
					:y1="region.rawTop + BOX_HEIGHT / 2"
					:x2="region.left + BOX_WIDTH / 2"
					:y2="region.top + BOX_HEIGHT / 2"
					stroke="#999"
					stroke-width="1" />
			</svg>

			<div
				v-for="region in positionedRegions"
				:key="region.region_id"
				class="regions-map-dot"
				:style="{left: `${region.rawLeft + BOX_WIDTH / 2}px`, top: `${region.rawTop + BOX_HEIGHT / 2}px`}" />

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
	overflow: auto;
	border: 1px solid #ccc;
}

.regions-map-inner {
	position: relative;
}

.regions-map-leaders {
	position: absolute;
	top: 0;
	left: 0;
	pointer-events: none;
}

.regions-map-dot {
	position: absolute;
	width: 6px;
	height: 6px;
	margin-left: -3px;
	margin-top: -3px;
	border-radius: 50%;
	background: #555;
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
