<script setup lang="ts">
import refdataApi from "~/refdata";

const props = defineProps<{
  value: string | number | undefined,
  attributeId: number | undefined
}>();

const {locale} = useI18n();

const attribute = await refdataApi.getDogmaAttribute({attributeId: props.attributeId});
</script>

<template>
  <template v-if="attribute">
    <template v-if="attribute.displayName && attribute.displayName[locale]">{{ attribute.displayName[locale] }}</template>
    <template v-else>{{attribute.name}}</template>
  </template>
  <span v-else>Unknown attribute</span>
  [{{ props.attributeId }}]: <DogmaValue :value="props.value" :attribute-id="props.attributeId" />
</template>
