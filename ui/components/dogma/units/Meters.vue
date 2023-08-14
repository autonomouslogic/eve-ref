<script setup lang="ts">
import refdataApi from "~/refdata";
import UnitValue from "~/components/dogma/UnitValue.vue";
import {DogmaAttribute, Unit} from "~/refdata-openapi";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";

const props = defineProps<{
	value: number | undefined
}>();

const {locale} = useI18n();

</script>

<template>
	<FormattedNumber :number="value" /> m
</template>
