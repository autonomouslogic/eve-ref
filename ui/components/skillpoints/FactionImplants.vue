<script setup lang="ts">
import {getJitaSellPrice} from "~/lib/marketUtils";
import refdataApi from "~/refdata";
import refdata from "~/refdata";
import {CYBERIMPLANT} from "~/lib/groupConstants";
import type {ImplantSet} from "~/components/skillpoints/ImplantSet";
import ImplantsTable from "~/components/skillpoints/ImplantsTable.vue";

const LOW_GRADE = "Low-grade";
const MID_GRADE = "Mid-grade";
const HIGH_GRADE = "High-grade";

const grades = [LOW_GRADE, MID_GRADE, HIGH_GRADE];

const ALPHA = "Alpha";
const BETA = "Beta";
const DELTA = "Delta";
const EPSILON = "Epsilon";
const GAMMA = "Gamma";

const slots = [ALPHA, BETA, DELTA, EPSILON, GAMMA];

const implantIds = (await refdata.getGroup({groupId: CYBERIMPLANT})).typeIds || [];

// Set grade -> position name -> [implant id , implant price]
const implantSets: {[key: string]: {[key: string]: number[]}} = {};

await Promise.allSettled(implantIds.map(async implantId => {
	const implant = await refdataApi.getType({typeId: implantId});
	if (!implant) {
		return;
	}
	const name = implant.name?.en;
	if (!name) {
		return;
	}

	let grade;
	if (name.startsWith(LOW_GRADE)) {
		grade = LOW_GRADE;
	} else if (name.startsWith(MID_GRADE)) {
		grade = MID_GRADE;
	} else if (name.startsWith(HIGH_GRADE)) {
		grade = HIGH_GRADE;
	} else {
		return;
	}

	const position = name.split(" ")[2];
	if (!position) {
		return;
	}
	if (position != ALPHA && position != BETA && position != DELTA && position != EPSILON && position != GAMMA) {
		return;
	}

	const price = await getJitaSellPrice(implantId) || 0;
	if (price <= 0) {
		return;
	}

	if (!implantSets[grade]) {
		implantSets[grade] = {};
	}
	if (!implantSets[grade][position]) {
		implantSets[grade][position] = [];
	}
	const current = implantSets[grade][position];

	if (current.length != 2 || current[1] > price) {
		current[0] = implantId;
		current[1] = price;
	}
}));

let bonus = 2;
const sets: ImplantSet[] =
	grades.map(grade => {
		return {
			name: grade,
			bonus: bonus++,
			typeIds: slots.map(slot => implantSets[grade][slot][0]),
		} as ImplantSet;
	});

</script>

<template>
	<h2>Faction Implants</h2>
	<p>
		Same as standard implants above, but tries to assemble the cheapest combination of faction implants available.
		This does not create matching sets, as the Omega slot is ignored and different sets may be mixed.
		It assumes you only care about skill points and not the other bonuses.
	</p>

	<ImplantsTable :sets="sets" />

</template>

<style scoped>
</style>
