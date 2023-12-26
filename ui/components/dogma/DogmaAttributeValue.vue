<script setup lang="ts">
import refdataApi from "~/refdata";
import {DogmaAttribute} from "~/refdata-openapi";
import AttributeTypeIcon from "~/components/icons/AttributeTypeIcon.vue";
import DogmaAttributeLink from "~/components/helpers/DogmaAttributeLink.vue";

const props = defineProps<{
	value: number,
	attribute: DogmaAttribute | number,
}>();

const dogmaAttribute: DogmaAttribute = typeof props.attribute === "number" ?
	await refdataApi.getDogmaAttribute({attributeId: props.attribute}) :
	props.attribute;
</script>

<template>
	<dt>
		<template v-if="dogmaAttribute">
			<AttributeTypeIcon :dogma-attribute="dogmaAttribute" :size="25" />
			<DogmaAttributeLink v-if="dogmaAttribute.attributeId" :attribute="dogmaAttribute.attributeId" />:
		</template>
	</dt>
	<dd>
		<DogmaValue :value="value" :attribute="dogmaAttribute" />
	</dd>
</template>

<style scoped>
dt {
}
dd {
  @apply border-gray-300 border-b pl-4 font-bold text-right;
}
</style>
