<script setup lang="ts">
import refdataApi from "~/refdata";
import UnitValue from "~/components/dogma/UnitValue.vue";

const {value, attributeId} = defineProps<{
  value: string | number | undefined,
  attributeId: number | undefined
}>();

const {locale} = useI18n();

const attribute = await refdataApi.getDogmaAttribute({attributeId});
</script>

<template>
  <UnitValue v-if="attribute.unitId" :value="value" :unit-id="attribute.unitId" />
  <span v-else>{{ value }}</span>
</template>
