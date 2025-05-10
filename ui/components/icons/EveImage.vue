<script setup lang="ts">
import {getTypeIconUrl} from "~/lib/urls";

const props = defineProps<{
	typeId: number
}>();

const iconUrl = props.typeId ? await getTypeIconUrl(props.typeId) : "";

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
