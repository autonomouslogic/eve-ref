<script setup lang="ts">
import refdataApi from "~/refdata";
import {DAY as DAY_MS, HOUR as HOUR_MS, SECOND as SECOND_MS} from "~/lib/timeUtils";
import AttibuteId from "~/components/dogma/units/AttibuteId.vue";
import Sizeclass from "~/components/dogma/units/Sizeclass.vue";
import DefaultUnit from "~/components/dogma/units/DefaultUnit.vue";
import AbsolutePercent from "~/components/dogma/units/AbsolutePercent.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";
import AbsoluteInveresePercent from "~/components/dogma/units/AbsoluteInveresePercent.vue";
import Duration from "~/components/dogma/units/Duration.vue";
import {
	ABSOLUTE_PERCENT,
	ATTRIBUTE_ID, BOOLEAN,
	DATETIME,
	GROUP_ID,
	HOURS,
	INVERSE_ABSOLUTE_PERCENT,
	MILLISECONDS,
	MONEY,
	SECOND,
	SIZE_CLASS,
	TRUE_TIME,
	TYPE_ID
} from "~/lib/unitConstants";
import GroupLink from "~/components/helpers/GroupLink.vue";
import Datetime from "~/components/dogma/units/Datetime.vue";
import Money from "~/components/dogma/units/Money.vue";
import Boolean from "~/components/dogma/units/Boolean.vue";

const props = defineProps<{
	value: number,
	unitId: number
}>();

const unit = await refdataApi.getUnit({unitId: props.unitId});
</script>

<template>
	<span v-if="!unit">{{ props.value }} (Unknown unit ID {{props.unitId}})</span>
	<template v-else>
		<AttibuteId v-if="unit.unitId == ATTRIBUTE_ID" :value="value" />
		<Sizeclass v-else-if="unit.unitId == SIZE_CLASS" :value="value" />
		<AbsolutePercent v-else-if="unit.unitId == ABSOLUTE_PERCENT" :value="value" />
		<AbsoluteInveresePercent v-else-if="unit.unitId == INVERSE_ABSOLUTE_PERCENT" :value="value" />
		<Money v-else-if="unit.unitId == MONEY" :value="value" />
		<GroupLink v-else-if="unit.unitId == GROUP_ID" :group-id="value" />
		<TypeLink v-else-if="unit.unitId == TYPE_ID" :type-id="value" />
		<Duration v-else-if="unit.unitId == HOURS" :milliseconds="value * HOUR_MS" />
		<Duration v-else-if="unit.unitId == SECOND" :milliseconds="value * SECOND_MS" />
		<Duration v-else-if="unit.unitId == TRUE_TIME" :milliseconds="value * SECOND_MS" />
		<Duration v-else-if="unit.unitId == MILLISECONDS" :milliseconds="value" />
		<Datetime v-else-if="unit.unitId == DATETIME" :millisecond-epoch="value * DAY_MS" />
		<Boolean v-else-if="unit.unitId == BOOLEAN" :value="value" />
		<DefaultUnit v-else :unit="unit" :value="value" />
	</template>
</template>
