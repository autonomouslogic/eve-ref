<script setup lang="ts">
import {getJitaSellPrice} from "~/lib/marketUtils";
import {MULTIPLE_PILOT_TRAINING_CERTIFICATE, PLEX_TYPE_ID} from "~/lib/typeConstants";
import Money from "~/components/dogma/units/Money.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";
import {
	AVERAGE_ATTRIBUTE,
	calculateSkillpoints,
	MAX_IMPLANT_BONUS,
	MAX_PRIMARY_ATTRIBUTE,
	MAX_SECONDARY_ATTRIBUTE
} from "~/lib/skillUtils";
import {DAY} from "~/lib/timeUtils";
import {mctPrices, omegaPrices} from "~/conf/newEdenStore";

const alphaSkillPointsPerMonth = calculateSkillpoints(AVERAGE_ATTRIBUTE, AVERAGE_ATTRIBUTE, 30 * DAY, false);
const alphaSkillPointsWithImplantsPerMonth = calculateSkillpoints(
	AVERAGE_ATTRIBUTE + MAX_IMPLANT_BONUS,
	AVERAGE_ATTRIBUTE + MAX_IMPLANT_BONUS,
	30 * DAY,
	false);
const maxAlphaSkillPointsPerMonth = calculateSkillpoints(MAX_PRIMARY_ATTRIBUTE, MAX_SECONDARY_ATTRIBUTE, 30 * DAY, false);
const maxAlphaSkillPointsWithImplantsPerMonth = calculateSkillpoints(
	MAX_PRIMARY_ATTRIBUTE + MAX_IMPLANT_BONUS,
	MAX_SECONDARY_ATTRIBUTE + MAX_IMPLANT_BONUS,
	30 * DAY,
	false);
const omegaSkillPointsPerMonth = calculateSkillpoints(AVERAGE_ATTRIBUTE, AVERAGE_ATTRIBUTE, 30 * DAY, true);
const omegaSkillPointsWithImplantsPerMonth = calculateSkillpoints(
	AVERAGE_ATTRIBUTE + MAX_IMPLANT_BONUS,
	AVERAGE_ATTRIBUTE + MAX_IMPLANT_BONUS,
	30 * DAY,
	true);
const maxOmegaSkillPointsPerMonth = calculateSkillpoints(MAX_PRIMARY_ATTRIBUTE, MAX_SECONDARY_ATTRIBUTE, 30 * DAY, true);
const maxOmegaSkillPointsWithImplantsPerMonth = calculateSkillpoints(
	MAX_PRIMARY_ATTRIBUTE + MAX_IMPLANT_BONUS,
	MAX_SECONDARY_ATTRIBUTE + MAX_IMPLANT_BONUS,
	30 * DAY,
	true);
const plexPrice = await getJitaSellPrice(PLEX_TYPE_ID) || 0;
const multiPrice = await getJitaSellPrice(MULTIPLE_PILOT_TRAINING_CERTIFICATE) || 0;
</script>

<template>
	<h2>Accounts</h2>
	<p>
		The price of implants is not included in the ISK/SP numbers, see below for details on implants.
	</p>
	<table class="table-auto w-full my-3">
		<thead>
			<th class="text-left"></th>
			<th class="text-left">Account Restriction</th>
			<th class="text-right">Price</th>
			<th class="text-left">Attributes</th>
			<th class="text-right">SP/mth</th>
			<th class="text-right">ISK/SP</th>
		</thead>

		<tbody>
			<tr>
				<td class="text-left" rowspan="3">Alpha</td>
				<td class="text-left" rowspan="3">Up to 5,000,000 SP</td>
				<td class="text-right" rowspan="3"><Money :value="0" /></td>
				<td class="text-left">{{ AVERAGE_ATTRIBUTE }}/{{ AVERAGE_ATTRIBUTE }}</td>
				<td class="text-right"><FormattedNumber :number="alphaSkillPointsPerMonth" /></td>
				<td class="text-right" rowspan="3"><Money :value="0" /></td>
			</tr>
			<tr>
				<td class="text-left">
					{{ MAX_PRIMARY_ATTRIBUTE }}/{{ MAX_SECONDARY_ATTRIBUTE }}
					<template v-if="alphaSkillPointsWithImplantsPerMonth == maxAlphaSkillPointsPerMonth">
						or
						{{ AVERAGE_ATTRIBUTE + MAX_IMPLANT_BONUS }}/{{ AVERAGE_ATTRIBUTE + MAX_IMPLANT_BONUS }}
						(+{{ MAX_IMPLANT_BONUS }} implants)
					</template>
				</td>
				<td class="text-right"><FormattedNumber :number="maxAlphaSkillPointsPerMonth" /></td>
			</tr>
			<tr>
				<td class="text-left">
					{{ MAX_PRIMARY_ATTRIBUTE + MAX_IMPLANT_BONUS }}/{{ MAX_SECONDARY_ATTRIBUTE + MAX_IMPLANT_BONUS }}
					(+{{ MAX_IMPLANT_BONUS }} implants)
				</td>
				<td class="text-right"><FormattedNumber :number="maxAlphaSkillPointsWithImplantsPerMonth" /></td>
			</tr>

			<tr v-for="(omega, idx) in omegaPrices" :key="omega.months">
				<td class="text-left">Omega ({{ omega.months }} month{{ omega.months > 1 ? "s" : "" }})</td>
				<td class="text-left"></td>
				<td class="text-right">
					<FormattedNumber :number="omega.plex" /> PLEX -
					<Money :value="omega.plex * plexPrice" />
				</td>
				<td class="text-left" v-if="idx == 0" :rowspan="omegaPrices.length">{{ AVERAGE_ATTRIBUTE }}/{{ AVERAGE_ATTRIBUTE }}</td>
				<td class="text-right" v-if="idx == 0" :rowspan="omegaPrices.length">
					<FormattedNumber :number="omegaSkillPointsPerMonth" />
				</td>
				<td class="text-right">
					<Money :value="omega.plex * plexPrice / (omega.months * omegaSkillPointsPerMonth)" />
				</td>
			</tr>

			<template v-for="(omega, idx) in omegaPrices" :key="omega.months">
				<tr v-if="idx == 0 || idx == omegaPrices.length - 1">
					<td class="text-left">Omega ({{ omega.months }} month{{ omega.months > 1 ? "s" : "" }})</td>
					<td class="text-left"></td>
					<td class="text-right">
						<FormattedNumber :number="omega.plex" /> PLEX -
						<Money :value="omega.plex * plexPrice" />
					</td>
					<td class="text-left" v-if="idx == 0" :rowspan="2">
						{{ MAX_PRIMARY_ATTRIBUTE }}/{{ MAX_SECONDARY_ATTRIBUTE }}
						<template v-if="omegaSkillPointsWithImplantsPerMonth == maxOmegaSkillPointsPerMonth">
							or
							{{ AVERAGE_ATTRIBUTE + MAX_IMPLANT_BONUS }}/{{ AVERAGE_ATTRIBUTE + MAX_IMPLANT_BONUS }}
							(+{{ MAX_IMPLANT_BONUS }} implants)
						</template>
					</td>
					<td class="text-right" v-if="idx == 0" :rowspan="2">
						<FormattedNumber :number="maxOmegaSkillPointsPerMonth" />
					</td>
					<td class="text-right">
						<Money :value="omega.plex * plexPrice / (omega.months * maxOmegaSkillPointsPerMonth)" />
					</td>
				</tr>
			</template>

			<template v-for="(omega, idx) in omegaPrices" :key="omega.months">
				<tr v-if="idx == 0 || idx == omegaPrices.length - 1">
					<td class="text-left">Omega ({{ omega.months }} month{{ omega.months > 1 ? "s" : "" }})</td>
					<td class="text-left"></td>
					<td class="text-right">
						<FormattedNumber :number="omega.plex" /> PLEX -
						<Money :value="omega.plex * plexPrice" />
					</td>
					<td class="text-left" v-if="idx == 0" :rowspan="2">
						{{ MAX_PRIMARY_ATTRIBUTE + MAX_IMPLANT_BONUS }}/{{ MAX_SECONDARY_ATTRIBUTE + MAX_IMPLANT_BONUS }}
						(+{{ MAX_IMPLANT_BONUS }} implants)
					</td>
					<td class="text-right" v-if="idx == 0" :rowspan="2">
						<FormattedNumber :number="maxOmegaSkillPointsWithImplantsPerMonth" />
					</td>
					<td class="text-right">
						<Money :value="omega.plex * plexPrice / (omega.months * maxOmegaSkillPointsWithImplantsPerMonth)" />
					</td>
				</tr>
			</template>

			<tr>
				<td class="text-left"><TypeLink :type-id="MULTIPLE_PILOT_TRAINING_CERTIFICATE" /> (Jita)</td>
				<td class="text-left" :rowspan="mctPrices.length + 1 + 2*2">Omega</td>
				<td class="text-right"><Money :value="multiPrice" /></td>
				<td class="text-left" :rowspan="mctPrices.length + 1">{{ AVERAGE_ATTRIBUTE }}/{{ AVERAGE_ATTRIBUTE }}</td>
				<td class="text-right" :rowspan="mctPrices.length + 1"><FormattedNumber :number="omegaSkillPointsPerMonth" /></td>
				<td class="text-right"><Money :value="multiPrice / omegaSkillPointsPerMonth" /></td>
			</tr>

			<tr v-for="mct in mctPrices" :key="mct.count">
				<td class="text-left">Multiple Character Training (x{{ mct.count }}, NES)</td>
				<td class="text-right">
					<FormattedNumber :number="mct.plex" /> PLEX -
					<Money :value="mct.plex * plexPrice" />
				</td>
				<td class="text-right"><Money :value="mct.plex * plexPrice / (mct.count * omegaSkillPointsPerMonth)" /></td>
			</tr>

			<template v-for="(mct, idx) in mctPrices" :key="mct.count">
				<tr v-if="idx == 0 || idx == omegaPrices.length - 1">
					<td class="text-left">Multiple Character Training (x{{ mct.count }}, NES)</td>
					<td class="text-right">
						<FormattedNumber :number="mct.plex" /> PLEX -
						<Money :value="mct.plex * plexPrice" />
					</td>
					<td class="text-left" v-if="idx == 0" rowspan="2">
						{{ MAX_PRIMARY_ATTRIBUTE }}/{{ MAX_SECONDARY_ATTRIBUTE }}
						<template v-if="omegaSkillPointsWithImplantsPerMonth == maxOmegaSkillPointsPerMonth">
							or
							{{ AVERAGE_ATTRIBUTE + MAX_IMPLANT_BONUS }}/{{ AVERAGE_ATTRIBUTE + MAX_IMPLANT_BONUS }}
							(+{{ MAX_IMPLANT_BONUS }} implants)
						</template>
					</td>
					<td class="text-right" v-if="idx == 0" rowspan="2"><FormattedNumber :number="maxOmegaSkillPointsPerMonth" /></td>
					<td class="text-right"><Money :value="mct.plex * plexPrice / (mct.count * maxOmegaSkillPointsPerMonth)" /></td>
				</tr>
			</template>

			<template v-for="(mct, idx) in mctPrices" :key="mct.count">
				<tr v-if="idx == 0 || idx == omegaPrices.length - 1">
					<td class="text-left">Multiple Character Training (x{{ mct.count }}, NES)</td>
					<td class="text-right">
						<FormattedNumber :number="mct.plex" /> PLEX -
						<Money :value="mct.plex * plexPrice" />
					</td>
					<td class="text-left" v-if="idx == 0" rowspan="2">
						{{ MAX_PRIMARY_ATTRIBUTE + MAX_IMPLANT_BONUS }}/{{ MAX_SECONDARY_ATTRIBUTE + MAX_IMPLANT_BONUS }}
						(+{{ MAX_IMPLANT_BONUS }} implants)
					</td>
					<td class="text-right" v-if="idx == 0" rowspan="2"><FormattedNumber :number="maxOmegaSkillPointsWithImplantsPerMonth" /></td>
					<td class="text-right"><Money :value="mct.plex * plexPrice / (mct.count * maxOmegaSkillPointsWithImplantsPerMonth)" /></td>
				</tr>
			</template>
		</tbody>
	</table>
</template>

<style scoped>
</style>
