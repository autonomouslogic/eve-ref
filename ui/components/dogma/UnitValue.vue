<script setup lang="ts">
import refdataApi from "~/refdata";
import AttibuteId from "~/components/dogma/units/AttibuteId.vue";
import Sizeclass from "~/components/dogma/units/Sizeclass.vue";
import DefaultUnit from "~/components/dogma/units/DefaultUnit.vue";
import AbsolutePercent from "~/components/dogma/units/AbsolutePercent.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";
import AbsoluteInveresePercent from "~/components/dogma/units/AbsoluteInveresePercent.vue";
import {
	ABSOLUTE_PERCENT,
	ATTRIBUTE_ID,
	GROUP_ID,
	INVERSE_ABSOLUTE_PERCENT,
	SIZE_CLASS,
	TYPE_ID
} from "~/lib/unitConstants";
import GroupLink from "~/components/helpers/GroupLink.vue";

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
		<GroupLink v-else-if="unit.unitId == GROUP_ID" :group-id="value" />
		<TypeLink v-else-if="unit.unitId == TYPE_ID" :type-id="value" />
		<DefaultUnit v-else :unit="unit" :value="value" />
	</template>
</template>
