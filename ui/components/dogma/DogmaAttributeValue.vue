<script setup lang="ts">
import refdataApi from "~/refdata";
import {DogmaAttribute} from "~/refdata-openapi";
import AttributeTypeIcon from "~/components/icons/AttributeTypeIcon.vue";
import DogmaAttributeLink from "~/components/helpers/DogmaAttributeLink.vue";

const props = defineProps<{
	value: string | number | undefined,
	attribute: DogmaAttribute | number,
}>();

const dogmaAttribute = typeof props.attribute === "number" ?
	await refdataApi.getDogmaAttribute({attributeId: props.attribute}) :
	props.attribute;
</script>

<template>
	<template v-if="dogmaAttribute">
		<AttributeTypeIcon :dogma-attribute="dogmaAttribute" :size="25" />
		<DogmaAttributeLink v-if="dogmaAttribute.attributeId" :attribute="dogmaAttribute.attributeId" />
	</template>
	<DogmaValue :value="props.value" :attribute-id="dogmaAttribute.attributeId" />
</template>
