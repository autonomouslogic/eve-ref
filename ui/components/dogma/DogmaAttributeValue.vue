<script setup lang="ts">
import refdataApi from "~/refdata";
import {DogmaAttribute} from "~/refdata-openapi";
import AttributeTypeIcon from "~/components/icons/AttributeTypeIcon.vue";
import DogmaAttributeLink from "~/components/helpers/DogmaAttributeLink.vue";

const props = defineProps<{
	value: string | number | undefined,
	attributeId: number | undefined
}>();

const {locale} = useI18n();

const attribute: DogmaAttribute = await refdataApi.getDogmaAttribute({attributeId: props.attributeId});
</script>

<template>
	<template v-if="attribute">
		<AttributeTypeIcon :dogma-attribute="attribute" :size="25" />
		<DogmaAttributeLink :attribute-id="props.attributeId" />
	</template>
	<span v-else>Unknown attribute {{ attributeId }}</span>:
	<DogmaValue :value="value" :attribute="attribute" />
</template>
