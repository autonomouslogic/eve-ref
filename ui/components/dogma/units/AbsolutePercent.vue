<script setup lang="ts">
import refdataApi from "~/refdata";
import UnitValue from "~/components/dogma/UnitValue.vue";
import {DogmaAttribute, Unit} from "~/refdata-openapi";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";

const props = defineProps<{
	value: string | number | undefined
}>();

</script>

<template>
	<FormattedNumber :number="value * 100.0" :decimals="2" />%
</template>
