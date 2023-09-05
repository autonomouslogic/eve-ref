<script setup lang="ts">
import refdataApi from "~/refdata";
import AttibuteId from "~/components/dogma/units/AttibuteId.vue";
import Sizeclass from "~/components/dogma/units/Sizeclass.vue";
import DefaultUnit from "~/components/dogma/units/DefaultUnit.vue";
import AbsolutePercent from "~/components/dogma/units/AbsolutePercent.vue";

const props = defineProps<{
	value: number,
	unitId: number
}>();

const {locale} = useI18n();

const unit = await refdataApi.getUnit({unitId: props.unitId});
</script>

<template>
	<span v-if="!unit">{{ props.value }} (Unknown unit ID {{props.unitId}})</span>
	<template v-else>
		<AttibuteId v-if="unit.unitId == 119" :value="value" />
		<Sizeclass v-else-if="unit.unitId == 117" :value="value" />
		<AbsolutePercent v-else-if="unit.unitId == 127" :value="value" />
		<DefaultUnit v-else :unit="unit" :value="value" />
	</template>
</template>
