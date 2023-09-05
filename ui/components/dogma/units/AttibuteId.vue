<script setup lang="ts">
import refdataApi from "~/refdata";

const props = defineProps<{
	value: string | number | undefined
}>();

const {locale} = useI18n();

const characterAttribute = await refdataApi.getDogmaAttribute({attributeId: props.value});

</script>

<template>
	<span v-if="characterAttribute">{{ characterAttribute.displayName[locale] }}</span>
	<span v-else>(Unknown character attribute ID {{value}})</span>
</template>
