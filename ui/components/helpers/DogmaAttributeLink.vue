<script setup lang="ts">
import refdataApi from "~/refdata";
import {type DogmaAttribute} from "~/refdata-openapi";
import {prepMessages} from "~/lib/translate";

const {locale} = useI18n();

const props = defineProps<{
	attribute: DogmaAttribute | number,
}>();

const dogmaAttribute = typeof props.attribute === "number" ?
	await refdataApi.getDogmaAttribute({attributeId: props.attribute}) :
	props.attribute;
</script>

<template>
	<NuxtLink
		v-if="dogmaAttribute"
		:to="`/dogma-attributes/${dogmaAttribute.attributeId}`">
		<slot>
			<template v-if="dogmaAttribute.displayName && prepMessages(dogmaAttribute.displayName)[locale]">
				{{ prepMessages(dogmaAttribute.displayName)[locale] }}
			</template>
			<template v-else>{{dogmaAttribute.name}}</template>
		</slot>
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
