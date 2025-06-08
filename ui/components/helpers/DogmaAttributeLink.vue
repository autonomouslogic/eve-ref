<script setup lang="ts">
import refdataApi from "~/refdata";
import {type DogmaAttribute} from "~/refdata-openapi";
import {tr} from "~/lib/translate";
import InternalLink from "~/components/helpers/InternalLink.vue";

const {locale} = useI18n();

const props = defineProps<{
	attribute: DogmaAttribute | number,
}>();

const dogmaAttribute = typeof props.attribute === "number" ?
	await refdataApi.getDogmaAttribute({attributeId: props.attribute}) :
	props.attribute;
</script>

<template>
	<InternalLink
		v-if="dogmaAttribute"
		:to="`/dogma-attributes/${dogmaAttribute.attributeId}`">
		<slot>
			<template v-if="dogmaAttribute.displayName && tr(dogmaAttribute.displayName, locale)">
				{{ tr(dogmaAttribute.displayName, locale) }}
			</template>
			<template v-else-if="dogmaAttribute.name">{{ dogmaAttribute.name }}</template>
			<span v-else class="italic">No name</span>
		</slot>
	</InternalLink>
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
