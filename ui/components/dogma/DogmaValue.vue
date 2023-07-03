<script setup lang="ts">
import refdataApi from "~/refdata";
import UnitValue from "~/components/dogma/UnitValue.vue";

const props = defineProps<{
  value: string | number | undefined,
  attributeId: number | undefined
}>();

const {locale} = useI18n();

const attribute = await refdataApi.getDogmaAttribute({attributeId: props.attributeId});
</script>

<template>
  <template v-if="attribute">
    <UnitValue v-if="attribute.unitId" :value="props.value" :unit-id="attribute.unitId" />
    <span v-else>{{ props.value }}</span>
  </template>
  <span v-else>(Unknown dogma attribute ID {{props.attributeId}})</span>
</template>
