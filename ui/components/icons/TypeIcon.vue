<script setup lang="ts">
import refdataApi from "~/refdata";
import {type DogmaAttribute, type Icon} from "~/refdata-openapi";
import {getTypeIconUrl} from "~/lib/urls";

const ownImageServer = "https://iec.jita.space";

const props = defineProps<{
	typeId: number
}>();

const iconUrl = computed(() => props.typeId ? getTypeIconUrl(props.typeId) : "");

</script>

<template>
	<img v-if="iconUrl" :src="iconUrl" />
	<template v-else>&nbsp;</template>
</template>

<style scoped>
img {
  display: inline-block;
}
</style>
