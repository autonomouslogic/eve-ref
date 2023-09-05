<script setup lang="ts">
import refdataApi from "~/refdata";
import {DogmaAttribute} from "~/refdata-openapi";

const {locale} = useI18n();

const props = defineProps<{
	attributeId: number
}>();

const attribute: DogmaAttribute = await refdataApi.getDogmaAttribute({attributeId: props.attributeId});
</script>

<template>
	<NuxtLink
		v-if="attribute"
		:to="`/dogma-attributes/${props.attributeId}`">
		<template v-if="attribute.displayName && attribute.displayName[locale]">{{ attribute.displayName[locale] }}</template>
		<template v-else>{{attribute.name}}</template>
	</NuxtLink>
</template>

<style scoped>
a {
  font-weight: normal;
  text-decoration: none;
  color: inherit;
}
a:hover {
  text-decoration: underline;
}
</style>
