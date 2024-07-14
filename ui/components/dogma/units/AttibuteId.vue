<script setup lang="ts">
import refdataApi from "~/refdata";
import {prepMessages} from "~/lib/translate";

const props = defineProps<{
	value: number
}>();

const {locale} = useI18n();

const characterAttribute = await refdataApi.getDogmaAttribute({attributeId: props.value});
</script>

<template>
	<span v-if="characterAttribute.displayName">{{ prepMessages(characterAttribute.displayName)[locale] }}</span>
	<span v-else>(Unknown character attribute ID {{value}})</span>
</template>
