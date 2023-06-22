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
  <template v-if="attribute.displayName && attribute.displayName[locale]">{{ attribute.displayName[locale] }}</template>
  <template v-else>{{attribute.name}}</template>
  [{{ attributeId }}]: <DogmaValue :value="value" :attribute-id="attributeId" />
</template>
