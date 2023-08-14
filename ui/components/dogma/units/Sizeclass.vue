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
	<template v-if="value == 1">small</template>
	<template v-else-if="value == 2">medium</template>
	<template v-else-if="value == 3">large</template>
	<template v-else-if="value == 4">x-large</template>
	<template v-else>{{ value }}</template>
</template>
